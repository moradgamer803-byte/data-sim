package com.zyvo.datasim;

import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

/**
 * Quick Settings tile.
 * Tap: data off -> data on (same SIM). data on -> switch to the other SIM.
 */
public class DataSimTile extends TileService {

    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    public void onStartListening() {
        super.onStartListening();
        refresh();
    }

    @Override
    public void onClick() {
        super.onClick();
        RootShell.EXEC.execute(new Runnable() {
            @Override
            public void run() {
                SimState st = SimState.read();
                if (!st.rootOk()) {
                    main.post(new Runnable() {
                        @Override
                        public void run() {
                            openApp();
                        }
                    });
                    return;
                }
                if (!st.mobileDataOn) {
                    SimState.setData(true);
                    final SimState after = SimState.read();
                    main.post(new Runnable() {
                        @Override
                        public void run() {
                            apply(after);
                            toast("Data on — " + after.dataSimName());
                        }
                    });
                    return;
                }
                final int next = otherSub(st);
                if (next <= 0) {
                    main.post(new Runnable() {
                        @Override
                        public void run() {
                            apply(st);
                            toast("Ekta SIM pai ni");
                        }
                    });
                    return;
                }
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        busy(nameOf(st, next));
                    }
                });
                SimState.switchTo(next);
                final SimState after = SimState.read();
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        apply(after);
                        toast("Data → " + after.dataSimName());
                    }
                });
            }
        });
    }

    // NOTE: TileService.onLongClick() only exists from API 35 — compileSdk 34
    // rejects the override, and this phone is Android 12 anyway. The picker
    // opens from the app icon (and from the tile when root is missing).

    private void openApp() {
        try {
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityAndCollapse(i);
        } catch (Exception ignored) {
        }
    }

    private void refresh() {
        if (getQsTile() == null) {
            return;
        }
        RootShell.EXEC.execute(new Runnable() {
            @Override
            public void run() {
                final SimState st = SimState.read();
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        apply(st);
                    }
                });
            }
        });
    }

    /**
     * Settled state = no glow. The pink "active" fill is used only while a
     * switch is running (see busy()), so the tile reads as a button, not as a
     * permanently lit indicator.
     */
    private void apply(SimState st) {
        Tile t = getQsTile();
        if (t == null) {
            return;
        }
        t.setState(Tile.STATE_INACTIVE);
        if (Build.VERSION.SDK_INT >= 29) {
            t.setLabel(getString(R.string.tile_label));
            if (!st.rootOk()) {
                t.setSubtitle("No root");
            } else if (!st.mobileDataOn) {
                t.setSubtitle("Data off");
            } else {
                t.setSubtitle(st.dataSimName());
            }
        }
        t.setIcon(Icon.createWithResource(this, R.drawable.ic_tile));
        t.updateTile();
    }

    /** In-flight state = pink glow, cleared by apply() when the switch ends. */
    private void busy(String targetName) {
        Tile t = getQsTile();
        if (t == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 29) {
            t.setLabel(getString(R.string.tile_label));
            t.setSubtitle("Switching → " + targetName);
        }
        t.setState(Tile.STATE_ACTIVE);
        t.updateTile();
    }

    private void toast(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

    private static int otherSub(SimState st) {
        if (st.sims.size() < 2) {
            return -1;
        }
        for (SimState.Sim s : st.sims) {
            if (s.subId != st.dataSubId) {
                return s.subId;
            }
        }
        return -1;
    }

    private static String nameOf(SimState st, int subId) {
        SimState.Sim s = st.simBySub(subId);
        return s == null ? ("SIM " + subId) : s.name;
    }
}
