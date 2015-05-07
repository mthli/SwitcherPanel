package io.github.mthli.SwitcherPanel;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

public class MainActivity extends Activity {
    private SwitcherPanel switcherPanel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        switcherPanel = (SwitcherPanel) findViewById(R.id.switcher_panel);

        final LinearLayout switcherContainer = (LinearLayout) findViewById(R.id.switcher_container);
        for (int i = 0; i < 16; i++) {
            final Button button = new Button(this);
            button.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
            button.setText(String.valueOf(i));
            button.setOnTouchListener(new SwipeToDismissListener(
                    button,
                    null,
                    new SwipeToDismissListener.DismissCallback() {
                        @Override
                        public boolean canDismiss(Object token) {
                            return true;
                        }

                        @Override
                        public void onDismiss(View view, Object token) {
                            switcherContainer.removeView(button);
                        }
                    }
            ));
            switcherContainer.addView(button);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.main_menu_collapsed:
                switcherPanel.collapsed();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(menuItem);
    }
}
