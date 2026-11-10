package com.zyvo.datasim;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView tvSimNow, tvSimMeta, tvRoot, tvLog, tvDataState;
    private TextView tvSimAName, tvSimAMeta, tvSimABadge;
    private TextView tvSimBName, tvSimBMeta, tvSimBBadge;
    private SwitchMaterial swData;
    private MaterialCardView cardSimA, cardSimB;

    private final Handler main = new Handler(Looper.getMainLooper());
    private SimState state = new SimState();
    private boolean busy = false;
    private boolean suppressSwitch = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvSimNow = findViewById(R.id.tvSimNow);
        tvSimMeta = findViewById(R.id.tvSimMeta);
        tvRoot = findViewById(R.id.tvRoot);
        tvLog = findViewById(R.id.tvLog);
        tvDataState = findViewById(R.id.tvDataState);
        tvSimAName = findViewById(R.id.tvSimAName);
        tvSimAMeta = findViewById(R.id.tvSimAMeta);
        tvSimABadge = findViewById(R.id.tvSimABadge);
        tvSimBName = findViewById(R.id.tvSimBName);
        tvSimBMeta = findViewById(R.id.tvSimBMeta);
        tvSimBBadge = findViewById(R.id.tvSimBBadge);
        swData = findViewById(R.id.swData);
        cardSimA = findViewById(R.id.cardSimA);
        cardSimB = findViewById(R.id.cardSimB);

        cardSimA.setOnClickListener(v -> {
            Object tag = v.getTag();
            if (tag instanceof Integer) {
                pick((Integer) tag);
            }
        });
        cardSimB.setOnClickListener(v -> {
            Object tag = v.getTag();
            if (tag instanceof Integer) {
                pick((Integer) tag);
            }
        });

        swData.setOnCheckedChangeListener((btn, checked) -> {
            if (suppressSwitch) {
                return;
            }
            setData(checked);
        });

        log("Root check hocche…");
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        RootShell.EXEC.execute(() -> {
            final SimState st = SimState.read();
            main.post(() -> applyState(st));
        });
    }

    private void applyState(SimState st) {
        state = st;
        tvRoot.setText(st.rootOk() ? "root ok" : "no root");
        tvSimNow.setText(st.dataSimName());
        tvSimMeta.setText((st.mobileDataOn ? "Data on" : "Data off")
                + " · data sub " + st.dataSubId);

        List<SimState.Sim> list = new ArrayList<>(st.sims);
        Collections.sort(list, (a, b) -> a.slot - b.slot);

        bindRow(list.size() > 0 ? list.get(0) : null,
                tvSimAName, tvSimAMeta, tvSimABadge, cardSimA);
        bindRow(list.size() > 1 ? list.get(1) : null,
                tvSimBName, tvSimBMeta, tvSimBBadge, cardSimB);

        suppressSwitch = true;
        swData.setChecked(st.mobileDataOn);
        suppressSwitch = false;
        tvDataState.setText(st.mobileDataOn ? "On — tile tap = switch SIM" : "Off — tile tap = data on");

        if (!st.rootOk()) {
            log("Root permission pai ni. APatch manager e app ta allow koro, then abar kholo.");
        } else if (busy) {
            log("Kaj cholche…");
        } else if (st.sims.isEmpty()) {
            log("Kono SIM detect hoy ni.");
        } else {
            log("Ready — " + st.sims.size() + " ta SIM, data ekhon " + st.dataSimName() + " e.");
        }
    }

    private void bindRow(@Nullable SimState.Sim sim, TextView name, TextView meta,
                         TextView badge, MaterialCardView card) {
        if (sim == null) {
            name.setText("SIM nai");
            meta.setText("Slot khali");
            badge.setText("");
            card.setTag(null);
            card.setEnabled(false);
            card.setAlpha(0.45f);
            return;
        }
        card.setEnabled(true);
        card.setAlpha(1f);
        card.setTag(sim.subId);
        boolean active = sim.subId == state.dataSubId;
        name.setText(sim.name);
        meta.setText("Slot " + (sim.slot + 1) + (active ? " · data e ache" : " · tap = ei SIM e data"));
        badge.setText(active ? "ACTIVE" : "");
    }

    private String nameOf(int subId) {
        SimState.Sim s = state.simBySub(subId);
        return s == null ? ("SIM " + subId) : s.name;
    }

    private void pick(int subId) {
        if (busy) {
            return;
        }
        if (subId == state.dataSubId) {
            log("Ei SIM agei data e ache.");
            return;
        }
        busy = true;
        log("Switching to " + nameOf(subId) + "…");
        RootShell.EXEC.execute(() -> {
            SimState.switchTo(subId);
            final SimState after = SimState.read();
            main.post(() -> {
                busy = false;
                applyState(after);
                if (after.dataSubId == subId) {
                    log("Done — data ekhon " + after.dataSimName() + " e.");
                    Toast.makeText(this, after.dataSimName() + " — data on", Toast.LENGTH_SHORT).show();
                } else {
                    log("Switch holo na — system " + after.dataSimName()
                            + " e ache. Oi SIM e network ache kina dekho.");
                }
            });
        });
    }

    private void setData(boolean on) {
        if (busy) {
            return;
        }
        busy = true;
        log(on ? "Data on korchi…" : "Data off korchi…");
        RootShell.EXEC.execute(() -> {
            SimState.setData(on);
            final SimState after = SimState.read();
            main.post(() -> {
                busy = false;
                applyState(after);
            });
        });
    }

    private void log(String s) {
        tvLog.setText(s);
    }
}
