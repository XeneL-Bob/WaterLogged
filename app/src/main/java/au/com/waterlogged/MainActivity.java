package au.com.waterlogged;

import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import au.com.waterlogged.ui.*;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/** Navigation hub: screens own their data flows, while this activity keeps the back stack tidy. */
public class MainActivity extends AppCompatActivity {
    private MaterialToolbar toolbar;
    private BottomNavigationView navigation;
    private Runnable pendingNavigation;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightNavigationBars(false);
        setContentView(R.layout.activity_main);
        View root = findViewById(R.id.root);
        ViewCompat.setOnApplyWindowInsetsListener(
                root,
                (v, insets) -> {
                    androidx.core.graphics.Insets bars =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                                            | WindowInsetsCompat.Type.ime());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return WindowInsetsCompat.CONSUMED;
                });
        toolbar = findViewById(R.id.toolbar);
        navigation = findViewById(R.id.navigation);
        toolbar.setNavigationOnClickListener(v -> back());
        toolbar.setOnMenuItemClickListener(
                item -> {
                    if (item.getItemId() == R.id.action_new) navigate(new SpeciesFragment(), true);
                    else if (item.getItemId() == R.id.action_help)
                        navigate(new HelpFragment(), true);
                    else if (item.getItemId() == R.id.action_sort) {
                        Fragment current =
                                getSupportFragmentManager().findFragmentById(R.id.content);
                        if (current instanceof CatchListFragment)
                            ((CatchListFragment) current).chooseSort();
                    }
                    return true;
                });
        navigation.setOnItemSelectedListener(
                item -> {
                    Fragment next;
                    if (item.getItemId() == R.id.nav_species) next = new SpeciesFragment();
                    else if (item.getItemId() == R.id.nav_settings) next = new SettingsFragment();
                    else next = CatchListFragment.create(item.getItemId() == R.id.nav_home);
                    navigate(next, false);
                    return false;
                });
        getOnBackPressedDispatcher()
                .addCallback(
                        this,
                        new OnBackPressedCallback(true) {
                            @Override
                            public void handleOnBackPressed() {
                                back();
                            }
                        });
        if (state == null) navigate(CatchListFragment.create(true), false);
    }

    public void chrome(String title, boolean detail, boolean list, int navId) {
        toolbar.setTitle(title);
        toolbar.getMenu().clear();
        if (list) toolbar.setLogo(R.drawable.ic_waterlogged_logo);
        else toolbar.setLogo((android.graphics.drawable.Drawable) null);
        if (detail) toolbar.setNavigationIcon(R.drawable.ic_back);
        else toolbar.setNavigationIcon((android.graphics.drawable.Drawable) null);
        toolbar.setNavigationContentDescription("Back");
        if (list) toolbar.inflateMenu(R.menu.list_actions);
        navigation.setVisibility(detail ? View.GONE : View.VISIBLE);
        if (navId != 0) navigation.getMenu().findItem(navId).setChecked(true);
    }

    public void navigate(Fragment next, boolean backStack) {
        Runnable go = () -> performNavigation(next, backStack);
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.content);
        if (current instanceof CatchItemFragment) ((CatchItemFragment) current).leave(go);
        else go.run();
    }

    private void performNavigation(Fragment next, boolean backStack) {
        if (getSupportFragmentManager().isStateSaved()) {
            pendingNavigation = () -> performNavigation(next, backStack);
            return;
        }
        if (!backStack)
            getSupportFragmentManager()
                    .popBackStackImmediate(
                            null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        androidx.fragment.app.FragmentTransaction transaction =
                getSupportFragmentManager().beginTransaction().replace(R.id.content, next);
        if (backStack) transaction.addToBackStack(null);
        transaction.commit();
    }

    public void back() {
        Runnable go = this::performBack;
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.content);
        if (current instanceof CatchItemFragment) ((CatchItemFragment) current).leave(go);
        else go.run();
    }

    private void performBack() {
        if (getSupportFragmentManager().isStateSaved()) {
            pendingNavigation = this::performBack;
            return;
        }
        if (getSupportFragmentManager().getBackStackEntryCount() > 0)
            getSupportFragmentManager().popBackStack();
        else finish();
    }

    @Override
    public void onPostResume() {
        super.onPostResume();
        if (pendingNavigation != null) {
            Runnable next = pendingNavigation;
            pendingNavigation = null;
            next.run();
        }
    }

    public void showLog() {
        navigate(CatchListFragment.create(false), false);
    }
}
