package com.example.freshguide.ui.admin;

import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.freshguide.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public abstract class BaseAdminBottomSheetFragment extends BottomSheetDialogFragment {

    @Override
    public int getTheme() {
        return R.style.ThemeOverlay_FreshGuide_BottomSheet;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!(getDialog() instanceof BottomSheetDialog)) {
            return;
        }

        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet == null) {
            return;
        }

        sheet.setBackgroundResource(R.drawable.bg_bottom_sheet_surface);
        applyBottomSheetDepth(sheet, 30);
        ViewGroup.LayoutParams params = sheet.getLayoutParams();
        if (params != null) {
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            sheet.setLayoutParams(params);
        }

        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
        behavior.setFitToContents(true);
        behavior.setSkipCollapsed(true);
        behavior.setDraggable(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void applyBottomSheetDepth(@NonNull View sheet, int elevationDp) {
        float density = getResources().getDisplayMetrics().density;
        float elevationPx = elevationDp * density;
        sheet.setElevation(elevationPx);
        sheet.setTranslationZ(elevationPx);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            sheet.setOutlineAmbientShadowColor(ContextCompat.getColor(requireContext(), R.color.bottom_sheet_shadow_ambient));
            sheet.setOutlineSpotShadowColor(ContextCompat.getColor(requireContext(), R.color.bottom_sheet_shadow_spot));
        }
    }
}
