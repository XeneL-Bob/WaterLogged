package au.com.waterlogged.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import au.com.waterlogged.MainActivity;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class HelpFragment extends Fragment {
    private WebView web;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater i, @Nullable ViewGroup p, @Nullable Bundle state) {
        LinearLayout root = Ui.column(requireContext());
        LinearLayout actions = Ui.row(requireContext());
        Ui.pad(actions, 8);
        Ui.weighted(actions, Ui.button(requireContext(), "App help", this::offline));
        Ui.weighted(
                actions,
                Ui.button(
                        requireContext(),
                        "QLD rules",
                        () ->
                                web.loadUrl(
                                        "https://www.qld.gov.au/recreation/activities/boating-fishing/rec-fishing/rules/limits-tidal")));
        root.addView(actions);
        web = new WebView(requireContext());
        web.setBackgroundColor(android.graphics.Color.rgb(3, 44, 75));
        web.getSettings().setJavaScriptEnabled(false);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.setWebViewClient(
                new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                        return !"https".equals(request.getUrl().getScheme());
                    }

                    @Override
                    public void onReceivedError(
                            WebView v, WebResourceRequest r, WebResourceError error) {
                        if (r.isForMainFrame())
                            v.loadDataWithBaseURL(
                                    "https://waterlogged.local/",
                                    "<html><body style='background:#032c4b;color:white;font:18px"
                                        + " sans-serif;padding:24px'><h2>Page"
                                        + " unavailable</h2><p>Check your internet connection, or"
                                        + " use App help above. Official pages may require"
                                        + " JavaScript or an external browser.</p></body></html>",
                                    "text/html",
                                    "UTF-8",
                                    null);
                    }
                });
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(
                Ui.button(
                        requireContext(),
                        "Assignment reference page",
                        () ->
                                web.loadUrl(
                                        "https://www.rta.qld.gov.au/before-renting/preparing-for-a-tenancy")));
        root.addView(
                Ui.button(
                        requireContext(),
                        "Data credits & licences",
                        () -> {
                            try (InputStream in =
                                    requireContext().getAssets().open("third_party_notices.txt")) {
                                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                                byte[] buffer = new byte[4096];
                                int n;
                                while ((n = in.read(buffer)) != -1) bytes.write(buffer, 0, n);
                                web.loadDataWithBaseURL(
                                        "https://waterlogged.local/",
                                        "<html><body"
                                                + " style='background:#032c4b;color:white;font:16px"
                                                + " sans-serif;padding:16px'><h2>Data credits &"
                                                + " licences</h2><pre style='white-space:pre-wrap'>"
                                                + android.text.TextUtils.htmlEncode(
                                                        new String(
                                                                bytes.toByteArray(),
                                                                StandardCharsets.UTF_8))
                                                + "</pre></body></html>",
                                        "text/html",
                                        "UTF-8",
                                        null);
                            } catch (Exception e) {
                                offline();
                            }
                        }));
        root.addView(
                Ui.button(
                        requireContext(),
                        "Species image credits",
                        () -> asset("species_credits.html")));
        offline();
        return root;
    }

    private void offline() {
        asset("help.html");
    }

    private void asset(String name) {
        try (InputStream in = requireContext().getAssets().open(name)) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) != -1) bytes.write(buffer, 0, n);
            web.loadDataWithBaseURL(
                    "https://waterlogged.local/",
                    new String(bytes.toByteArray(), StandardCharsets.UTF_8),
                    "text/html",
                    "UTF-8",
                    null);
        } catch (Exception e) {
            web.loadData(
                    "<h2>Water Logged help</h2><p>Choose a species, enter your catch details, then"
                            + " save to your log.</p>",
                    "text/html",
                    "UTF-8");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity) requireActivity()).chrome("Help & fishing rules", true, false, 0);
    }

    @Override
    public void onDestroyView() {
        if (web != null) {
            web.stopLoading();
            web.setWebViewClient(new WebViewClient());
            web.destroy();
            web = null;
        }
        super.onDestroyView();
    }
}
