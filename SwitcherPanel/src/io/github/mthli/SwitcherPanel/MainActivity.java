package io.github.mthli.SwitcherPanel;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {
    private SwitcherPanel switcherPanel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        switcherPanel = (SwitcherPanel) findViewById(R.id.switcher_panel);
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
