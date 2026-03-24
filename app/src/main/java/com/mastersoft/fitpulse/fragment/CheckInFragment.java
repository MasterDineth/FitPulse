package com.mastersoft.fitpulse.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.mastersoft.fitpulse.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CheckInFragment extends Fragment {

    // ── Day state constants ──────────────────────────────────────────────────
    public static final int STATE_FUTURE   = 0;   // grey dot
    public static final int STATE_TODAY    = 1;   // pin icon, secondaryContainer bg
    public static final int STATE_ATTENDED = 2;   // check icon, primary bg
    public static final int STATE_MISSED   = 3;   // dot icon, errorContainer bg

    // QR code size in pixels
    private static final int QR_SIZE = 600;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_check_in, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ── QR code generation ───────────────────────────────────────────────
        ImageView ivQrCode               = view.findViewById(R.id.ivQrCode);
        CircularProgressIndicator loader = view.findViewById(R.id.qrLoadingIndicator);

        generateQrCode(ivQrCode, loader);

        // ── Manual check-in button ───────────────────────────────────────────
        view.findViewById(R.id.btnManualCheckIn).setOnClickListener(v ->
                Snackbar.make(view, "Check-in recorded!", Snackbar.LENGTH_SHORT)
                        .setAnchorView(v)
                        .show()
        );

        // ── Week calendar ────────────────────────────────────────────────────
        int[] dayIncludeIds = {
                R.id.dayMon,
                R.id.dayTue,
                R.id.dayWed,
                R.id.dayThu,
                R.id.dayFri,
                R.id.daySat,
                R.id.daySun
        };

        String[] dayLabels = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

        int[] attendanceStates = {
                STATE_ATTENDED, // Mon
                STATE_ATTENDED, // Tue
                STATE_ATTENDED, // Wed
                STATE_ATTENDED, // Thu
                STATE_TODAY,    // Fri  ← today
                STATE_FUTURE,   // Sat
                STATE_FUTURE    // Sun
        };

        for (int i = 0; i < dayIncludeIds.length; i++) {
            View dayView = view.findViewById(dayIncludeIds[i]);
            applyDayState(dayView, dayLabels[i], attendanceStates[i]);
        }
    }

    private void generateQrCode(ImageView ivQrCode, CircularProgressIndicator loader) {
        loader.setVisibility(View.VISIBLE);
        ivQrCode.setVisibility(View.INVISIBLE);

        String userId    = "USR_12345";
        String date      = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String timestamp = String.valueOf(System.currentTimeMillis());
        String qrPayload = userId + "|" + date + "|" + timestamp;

        try {
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap bitmap = encoder.encodeBitmap(qrPayload, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
            bitmap = tintQrBitmap(bitmap);

            ivQrCode.setImageBitmap(bitmap);
            loader.setVisibility(View.GONE);
            ivQrCode.setVisibility(View.VISIBLE);

            ivQrCode.setAlpha(0f);
            ivQrCode.animate().alpha(1f).setDuration(400).start();

        } catch (WriterException e) {
            loader.setVisibility(View.GONE);
            e.printStackTrace();
        }
    }

    private Bitmap tintQrBitmap(Bitmap source) {
        int darkColor  = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurface, Color.BLACK);
        int lightColor = Color.TRANSPARENT;

        Bitmap tinted = source.copy(Bitmap.Config.ARGB_8888, true);
        for (int x = 0; x < tinted.getWidth(); x++) {
            for (int y = 0; y < tinted.getHeight(); y++) {
                int pixel = tinted.getPixel(x, y);
                tinted.setPixel(x, y, pixel == Color.BLACK ? darkColor : lightColor);
            }
        }
        return tinted;
    }

    private void applyDayState(View dayView, String label, int state) {
        if (dayView == null || getContext() == null) return;

        // Since we use <include android:id="..."> in fragment_check_in.xml, the root 
        // view's ID (dayCard) in item_day_card.xml is overridden by the include's ID 
        // (e.g., R.id.dayMon). Thus, dayView IS the MaterialCardView itself.
        MaterialCardView card;
        if (dayView instanceof MaterialCardView) {
            card = (MaterialCardView) dayView;
        } else {
            card = dayView.findViewById(R.id.dayCard);
        }

        if (card == null) return;

        TextView tvLabel       = dayView.findViewById(R.id.tvDayLabel);
        ImageView ivStatus     = dayView.findViewById(R.id.ivDayStatus);

        if (tvLabel != null) tvLabel.setText(label);

        int bgColor;
        int contentColor;
        int iconRes;

        switch (state) {
            case STATE_ATTENDED:
                bgColor = MaterialColors.getColor(card, com.google.android.material.R.attr.colorPrimaryContainer);
                contentColor = MaterialColors.getColor(card, com.google.android.material.R.attr.colorOnPrimaryContainer);
                iconRes = R.drawable.ic_day_check;
                card.setAlpha(1f);
                break;

            case STATE_TODAY:
                bgColor = MaterialColors.getColor(card, com.google.android.material.R.attr.colorSecondaryContainer);
                contentColor = MaterialColors.getColor(card, com.google.android.material.R.attr.colorOnSecondaryContainer);
                iconRes = R.drawable.ic_day_pin;
                card.setAlpha(1f);
                break;

            case STATE_MISSED:
                bgColor = MaterialColors.getColor(card, com.google.android.material.R.attr.colorErrorContainer);
                contentColor = MaterialColors.getColor(card, com.google.android.material.R.attr.colorOnErrorContainer);
                iconRes = R.drawable.ic_day_dot;
                card.setAlpha(1f);
                break;

            case STATE_FUTURE:
            default:
                bgColor = MaterialColors.getColor(card, com.google.android.material.R.attr.colorSurfaceContainerHighest);
                contentColor = MaterialColors.getColor(card, com.google.android.material.R.attr.colorOnSurfaceVariant);
                iconRes = R.drawable.ic_day_dot;
                card.setAlpha(0.55f);
                break;
        }

        card.setCardBackgroundColor(bgColor);
        if (tvLabel != null) tvLabel.setTextColor(contentColor);
        if (ivStatus != null) {
            ivStatus.setImageResource(iconRes);
            ivStatus.setColorFilter(contentColor);
        }
    }

    public static CheckInFragment newInstance() {
        return new CheckInFragment();
    }
}
