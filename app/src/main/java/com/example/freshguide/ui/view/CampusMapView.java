package com.example.freshguide.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Campus map rendered as isometric-style faceted buildings.
 *
 * Building coordinates are normalised [0..1] × [0..1] in a SQUARE design space.
 * That square is fitted to view-width and centred vertically, so proportions
 * are preserved on any screen size or aspect ratio.
 */
public class CampusMapView extends View {

    public interface OnBuildingClickListener {
        void onBuildingClick(String buildingCode, String buildingName);
    }

    // ── Data ──────────────────────────────────────────────────────────────

    private static class BuildingShape {
        final String code;
        final String name;
        /** 8 floats: TL-x, TL-y, TR-x, TR-y, BR-x, BR-y, BL-x, BL-y  (normalised 0..1) */
        final float[] pts;
        final int baseColor;

        BuildingShape(String code, String name, float[] pts, int baseColor) {
            this.code = code; this.name = name;
            this.pts = pts;   this.baseColor = baseColor;
        }
    }

    // ── Map transform (recomputed on size change) ─────────────────────────
    // normalised (0..1) → pixel:   pixel = norm * mapScale + mapOff{X|Y}

    private float mapScale = 1f;   // 1 normalised unit in pixels  (= view width)
    private float mapOffX  = 0f;   // horizontal offset (0 when view is wide)
    private float mapOffY  = 0f;   // vertical offset   (centres square in tall view)

    // ── State ─────────────────────────────────────────────────────────────

    private final List<BuildingShape> buildings = new ArrayList<>();

    private final Paint pFill   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pLabel  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pAccent = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pMarkerOuter = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pMarkerInner = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pMarkerCore  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pDashed = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pPin = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pPinInner = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pBg     = new Paint();

    private OnBuildingClickListener clickListener;

    private float panX = 0f, panY = 0f, zoom = 1f;

    private final GestureDetector      gestureDetector;
    private final ScaleGestureDetector scaleDetector;

    // ── Construction ──────────────────────────────────────────────────────

    public CampusMapView(Context context) { this(context, null); }

    public CampusMapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initBuildings();
        initPaints();

        gestureDetector = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override public boolean onScroll(
                            MotionEvent e1, MotionEvent e2, float dX, float dY) {
                        panX -= dX; panY -= dY; clampPan(); invalidate(); return true;
                    }
                    @Override public boolean onSingleTapUp(MotionEvent e) {
                        handleTap(e.getX(), e.getY()); return true;
                    }
                });

        scaleDetector = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override public boolean onScale(ScaleGestureDetector d) {
                        zoom = Math.max(0.8f, Math.min(3.5f, zoom * d.getScaleFactor()));
                        clampPan(); invalidate(); return true;
                    }
                });
    }

    // ── Size change → recompute map transform ─────────────────────────────

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        computeMapTransform(w, h);
    }

    /**
     * Buildings are defined in a 1×1 square. We fit that square to the
     * narrower dimension and centre it along the longer dimension.
     */
    private void computeMapTransform(int w, int h) {
        if (w <= 0 || h <= 0) return;
        mapScale = Math.min(w * 0.98f, h * 0.98f);
        mapOffX  = (w - mapScale) / 2f;
        mapOffY  = (h - mapScale) / 2f - mapScale * 0.08f;
    }

    // ── Building layout ───────────────────────────────────────────────────
    // Normalised [0..1] within a SQUARE design canvas (1 unit = view width).
    // Proportions measured directly from the target design image.
    //
    //  MAIN = tall portrait block on the left (30% wide × 64% tall)
    //  REGISTRAR = small landscape block upper-right (26% wide × 16% tall)
    //  COURT = medium square centre (32% × 28%)
    //  LIBRARY = medium landscape right (28% × 28%)
    //  ENTRANCE = thin portrait strip far-left (14% × 16%)
    //  EXIT = small block centre-right bottom (18% × 14%)

    private void initBuildings() {
        // MAIN — tall portrait, left (drawn first = behind COURT)
        buildings.add(new BuildingShape("MAIN", "UCC - SOUTH MAIN\nBUILDING",
                new float[]{ 0.08f,0.26f, 0.44f,0.26f, 0.42f,0.86f, 0.06f,0.86f },
                Color.parseColor("#4FAE63")));

        // REGISTRAR — small landscape, upper-right
        buildings.add(new BuildingShape("REG", "REGISTRAR",
                new float[]{ 0.48f,0.14f, 0.78f,0.14f, 0.76f,0.26f, 0.46f,0.26f },
                Color.parseColor("#6CC978")));

        // COURT — medium, centre (sits to the right of MAIN)
        buildings.add(new BuildingShape("COURT", "COURT",
                new float[]{ 0.50f,0.30f, 0.88f,0.30f, 0.86f,0.70f, 0.48f,0.70f },
                Color.parseColor("#7ACB86")));

        // LIBRARY — medium landscape, right
        buildings.add(new BuildingShape("LIB", "LIBRARY",
                new float[]{ 0.70f,0.71f, 0.86f,0.71f, 0.84f,0.87f, 0.68f,0.87f },
                Color.parseColor("#6CC978")));

        // ENTRANCE — compact grey block, lower-left
        buildings.add(new BuildingShape("ENT", "ENTRANCE",
                new float[]{ 0.26f,0.90f, 0.42f,0.90f, 0.42f,1.00f, 0.26f,1.00f },
                Color.parseColor("#F2F2F2")));

        // EXIT — small grey block, bottom-right
        buildings.add(new BuildingShape("EXIT", "EXIT",
                new float[]{ 0.60f,0.90f, 0.76f,0.90f, 0.76f,1.00f, 0.60f,1.00f },
                Color.parseColor("#F4F4F4")));

    }

    private void initPaints() {
        pFill.setStyle(Paint.Style.FILL);

        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setColor(Color.WHITE);
        pStroke.setStrokeWidth(2.0f);

        pLabel.setStyle(Paint.Style.FILL);
        pLabel.setTextAlign(Paint.Align.CENTER);
        pLabel.setFakeBoldText(false);
        pLabel.setColor(Color.parseColor("#6A6A6A"));

        pAccent.setStyle(Paint.Style.FILL);
        pAccent.setColor(Color.parseColor("#FF6D00"));

        pMarkerOuter.setStyle(Paint.Style.FILL);
        pMarkerOuter.setColor(Color.parseColor("#1E88E5"));

        pMarkerInner.setStyle(Paint.Style.FILL);
        pMarkerInner.setColor(Color.WHITE);

        pMarkerCore.setStyle(Paint.Style.FILL);
        pMarkerCore.setColor(Color.parseColor("#D9D9D9"));

        pDashed.setStyle(Paint.Style.STROKE);
        pDashed.setColor(Color.parseColor("#BDBDBD"));

        pPin.setStyle(Paint.Style.FILL);
        pPin.setColor(Color.parseColor("#29A829"));

        pPinInner.setStyle(Paint.Style.FILL);
        pPinInner.setColor(Color.WHITE);

        pBg.setColor(Color.WHITE);
        pBg.setStyle(Paint.Style.FILL);
    }

    // ── Drawing ───────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;

        canvas.drawRect(0, 0, w, h, pBg);

        canvas.save();
        // Zoom around the centre of the view, then apply pan
        canvas.translate(w / 2f * (1 - zoom) + panX, h / 2f * (1 - zoom) + panY);
        canvas.scale(zoom, zoom);

        pLabel.setTextSize(mapScale * 0.0205f);
        // Stroke widths scale with map size
        pStroke.setStrokeWidth(mapScale * 0.004f);
        pDashed.setStrokeWidth(mapScale * 0.0035f);
        pDashed.setPathEffect(new DashPathEffect(
                new float[]{ mapScale * 0.012f, mapScale * 0.008f }, 0));

        drawAccentBars(canvas);

        // Pass 1: all building shapes (no labels)
        for (BuildingShape b : buildings) {
            drawBuildingShape(canvas, b);
        }
        // Pass 2: all labels on top so no shape can cover them
        for (BuildingShape b : buildings) {
            drawBuildingLabel(canvas, b);
        }

        drawUserMarker(canvas);

        canvas.restore();
    }

    /** Orange pathway markers to match the home-map composition. */
    private void drawAccentBars(Canvas canvas) {
        float bw = mapScale * 0.012f;
        float leftX = mapOffX + mapScale * 0.04f;
        canvas.drawRect(leftX, mapOffY + mapScale * 0.12f,
                leftX + bw, mapOffY + mapScale * 0.30f, pAccent);
        canvas.drawRect(leftX, mapOffY + mapScale * 0.86f,
                leftX + bw, mapOffY + mapScale * 1.00f, pAccent);

        float barTop = mapOffY + mapScale * 0.92f;
        canvas.drawRect(mapOffX + mapScale * 0.40f, barTop,
                mapOffX + mapScale * 0.86f, barTop + bw, pAccent);
    }

    /** Draws only the filled shape + outline (no label). */
    private void drawBuildingShape(Canvas canvas, BuildingShape b) {
        float[] p = toPixels(b.pts);
        if ("REG".equals(b.code)) {
            drawRegistrarShape(canvas, p, b.baseColor);
            return;
        }
        if ("ENT".equals(b.code) || "EXIT".equals(b.code)) {
            drawFlatQuad(canvas, p, b.baseColor);
            canvas.drawPath(quad(p), pDashed);
            return;
        }

        float cx = (p[0] + p[2] + p[4] + p[6]) / 4f;
        float cy = (p[1] + p[3] + p[5] + p[7]) / 4f;

        int light = blend(b.baseColor, Color.WHITE, 0.45f);
        int mid   = blend(b.baseColor, Color.WHITE, 0.15f);
        int dark  = blend(b.baseColor, Color.BLACK, 0.22f);

        drawTri(canvas, p[0],p[1], p[2],p[3], cx,cy, light);
        drawTri(canvas, p[2],p[3], p[4],p[5], cx,cy, mid);
        drawTri(canvas, p[4],p[5], p[6],p[7], cx,cy, light);
        drawTri(canvas, p[6],p[7], p[0],p[1], cx,cy, dark);

        canvas.drawPath(quad(p), pStroke);
    }

    /** Draws only the label — always on top of all shapes. */
    private void drawBuildingLabel(Canvas canvas, BuildingShape b) {
        if (b.name == null || b.name.trim().isEmpty()) return;
        float[] p = toPixels(b.pts);
        String[] lines = b.name.split("\n");
        int lineCount = lines.length;
        float lineHeight = pLabel.getTextSize() * 1.25f;
        Paint.FontMetrics fm = pLabel.getFontMetrics();
        float centerX = (p[0] + p[2]) / 2f;
        float topEdge = Math.min(Math.min(p[1], p[3]), Math.min(p[5], p[7]));
        float bottomEdge = Math.max(Math.max(p[1], p[3]), Math.max(p[5], p[7]));
        float labelX = centerX;
        float labelY;

        switch (b.code) {
            case "REG":
                labelY = topEdge - pLabel.getTextSize() * 0.42f - fm.descent - lineHeight * (lineCount - 1);
                break;
            case "MAIN":
                labelY = bottomEdge + pLabel.getTextSize() * 0.36f - fm.ascent;
                break;
            case "COURT":
                labelY = bottomEdge + pLabel.getTextSize() * 0.24f - fm.ascent;
                break;
            case "LIB":
                labelY = bottomEdge + pLabel.getTextSize() * 0.28f - fm.ascent;
                break;
            case "EXIT":
                labelY = topEdge + (bottomEdge - topEdge) * 0.52f - (fm.ascent + fm.descent) / 2f;
                break;
            case "ENT":
                labelX = centerX - mapScale * 0.06f;
                labelY = bottomEdge + pLabel.getTextSize() * 0.18f - fm.ascent;
                break;
            default:
                labelY = topEdge - pLabel.getTextSize() * 0.25f - fm.descent - lineHeight * (lineCount - 1);
                break;
        }

        labelY = Math.max(mapOffY - fm.ascent, labelY);
        drawPinnedLabel(canvas, b.name, labelX, labelY);
    }

    private void drawPinnedLabel(Canvas canvas, String text, float centerX, float y) {
        String[] lines = text.split("\n");
        float lineHeight = pLabel.getTextSize() * 1.3f;
        Paint.FontMetrics fm = pLabel.getFontMetrics();

        float maxWidth = 0f;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, pLabel.measureText(line));
        }

        float startX = centerX - maxWidth / 2f + mapScale * 0.01f;
        float topY = y + fm.ascent;
        float bottomY = y + fm.descent + lineHeight * (lines.length - 1);
        float centerY = (topY + bottomY) / 2f;

        float r = mapScale * 0.011f;
        float pinGap = mapScale * 0.008f;
        float pinX = startX - pinGap - r;
        float pinTipY = centerY + r * 0.9f;

        drawPin(canvas, pinX, pinTipY, r);

        Paint.Align prev = pLabel.getTextAlign();
        pLabel.setTextAlign(Paint.Align.LEFT);
        float textY = y;
        for (String line : lines) {
            canvas.drawText(line, startX, textY, pLabel);
            textY += lineHeight;
        }
        pLabel.setTextAlign(prev);
    }

    private void drawPin(Canvas canvas, float x, float tipY, float r) {
        float circleY = tipY - r * 1.4f;

        Path tri = new Path();
        tri.moveTo(x, tipY);
        tri.lineTo(x - r * 0.9f, tipY - r * 0.8f);
        tri.lineTo(x + r * 0.9f, tipY - r * 0.8f);
        tri.close();

        canvas.drawCircle(x, circleY, r, pPin);
        canvas.drawPath(tri, pPin);
        canvas.drawCircle(x, circleY, r * 0.45f, pPinInner);
    }

    private void drawUserMarker(Canvas canvas) {
        // Hidden for now per UI target
    }

    private void drawRegistrarShape(Canvas canvas, float[] p, int color) {
        float gap = mapScale * 0.025f;
        float width = (p[2] - p[0] - gap) / 2f;
        float[] left = new float[]{
                p[0], p[1],
                p[0] + width, p[1],
                p[0] + width - mapScale * 0.02f, p[5],
                p[0] - mapScale * 0.02f, p[5]
        };
        float[] right = new float[]{
                p[0] + width + gap, p[1],
                p[2], p[1],
                p[4], p[5],
                p[0] + width + gap - mapScale * 0.02f, p[5]
        };
        drawFacetedQuad(canvas, left, color);
        drawFacetedQuad(canvas, right, color);
    }

    private void drawFacetedQuad(Canvas canvas, float[] p, int baseColor) {
        float cx = (p[0] + p[2] + p[4] + p[6]) / 4f;
        float cy = (p[1] + p[3] + p[5] + p[7]) / 4f;

        int light = blend(baseColor, Color.WHITE, 0.45f);
        int mid   = blend(baseColor, Color.WHITE, 0.15f);
        int dark  = blend(baseColor, Color.BLACK, 0.22f);

        drawTri(canvas, p[0],p[1], p[2],p[3], cx,cy, light);
        drawTri(canvas, p[2],p[3], p[4],p[5], cx,cy, mid);
        drawTri(canvas, p[4],p[5], p[6],p[7], cx,cy, light);
        drawTri(canvas, p[6],p[7], p[0],p[1], cx,cy, dark);
        canvas.drawPath(quad(p), pStroke);
    }

    private void drawFlatQuad(Canvas canvas, float[] p, int fillColor) {
        pFill.setColor(fillColor);
        canvas.drawPath(quad(p), pFill);
        canvas.drawPath(quad(p), pStroke);
    }

    private void drawTri(Canvas canvas,
                         float ax, float ay, float bx, float by,
                         float cx, float cy, int color) {
        Path p = new Path();
        p.moveTo(ax, ay); p.lineTo(bx, by); p.lineTo(cx, cy); p.close();
        pFill.setColor(color);
        canvas.drawPath(p, pFill);
    }

    private void drawLabel(Canvas canvas, String text, float cx, float y) {
        for (String line : text.split("\n")) {
            canvas.drawText(line, cx, y, pLabel);
            y += pLabel.getTextSize() * 1.3f;
        }
    }

    private void drawVerticalLabel(Canvas canvas, String text, float cx, float cy) {
        canvas.save();
        canvas.rotate(-90f, cx, cy);
        canvas.drawText(text, cx, cy, pLabel);
        canvas.restore();
    }

    // ── Coordinate helpers ────────────────────────────────────────────────

    /**
     * Converts normalised building points to pixel coordinates using the
     * current map transform (mapScale + mapOff).
     */
    private float[] toPixels(float[] norm) {
        float[] out = new float[8];
        for (int i = 0; i < 4; i++) {
            out[i * 2]     = norm[i * 2]     * mapScale + mapOffX;
            out[i * 2 + 1] = norm[i * 2 + 1] * mapScale + mapOffY;
        }
        return out;
    }

    private Path quad(float[] p) {
        Path path = new Path();
        path.moveTo(p[0],p[1]); path.lineTo(p[2],p[3]);
        path.lineTo(p[4],p[5]); path.lineTo(p[6],p[7]); path.close();
        return path;
    }

    private int blend(int c1, int c2, float t) {
        int r = (int)(Color.red(c1)   + (Color.red(c2)   - Color.red(c1))   * t);
        int g = (int)(Color.green(c1) + (Color.green(c2) - Color.green(c1)) * t);
        int b = (int)(Color.blue(c1)  + (Color.blue(c2)  - Color.blue(c1))  * t);
        return Color.rgb(clamp(r), clamp(g), clamp(b));
    }

    private int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    // ── Touch ─────────────────────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        return true;
    }

    private void handleTap(float rawX, float rawY) {
        int w = getWidth(), h = getHeight();
        // Undo the canvas zoom/pan transform
        float tx = w / 2f * (1 - zoom) + panX;
        float ty = h / 2f * (1 - zoom) + panY;
        float worldX = (rawX - tx) / zoom;
        float worldY = (rawY - ty) / zoom;

        for (BuildingShape b : buildings) {
            float[] p = toPixels(b.pts);
            Path path = quad(p);
            RectF bounds = new RectF();
            path.computeBounds(bounds, true);
            Region region = new Region((int)bounds.left,(int)bounds.top,
                                       (int)bounds.right,(int)bounds.bottom);
            region.setPath(path, region);
            if (region.contains((int)worldX, (int)worldY)) {
                if (clickListener != null)
                    clickListener.onBuildingClick(b.code, b.name.replace("\n", " "));
                return;
            }
        }
    }

    private void clampPan() {
        int w = getWidth(), h = getHeight();
        float max = Math.max(0f, (zoom - 1f) * Math.max(w, h) * 0.5f);
        panX = Math.max(-max, Math.min(max, panX));
        panY = Math.max(-max, Math.min(max, panY));
    }

    // ── Public API ────────────────────────────────────────────────────────

    public void setOnBuildingClickListener(OnBuildingClickListener l) { clickListener = l; }

    public void resetView() {
        panX = 0f; panY = 0f; zoom = 1f;
        invalidate();
    }
}
