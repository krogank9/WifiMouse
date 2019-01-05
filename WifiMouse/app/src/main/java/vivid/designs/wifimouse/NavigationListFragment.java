package vivid.designs.wifimouse;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.solovyev.android.checkout.ActivityCheckout;
import org.solovyev.android.checkout.Billing;
import org.solovyev.android.checkout.BillingRequests;
import org.solovyev.android.checkout.Checkout;
import org.solovyev.android.checkout.EmptyRequestListener;
import org.solovyev.android.checkout.Inventory;
import org.solovyev.android.checkout.ProductTypes;
import org.solovyev.android.checkout.Purchase;

import java.util.List;

public class NavigationListFragment extends Fragment {

    public NavigationListFragment() {}

    static String[] donateTiers = {"$1", "$5", "$10", "$20"};

    private class PurchaseListener extends EmptyRequestListener<Purchase> {
        @Override
        public void onSuccess(Purchase purchase) {
            Toast.makeText(getContext(), "Thank you for your donation!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onError(int response, Exception e) {}
    }

    private class InventoryCallback implements Inventory.Callback {
        @Override
        public void onLoaded(Inventory.Products products) {}
    }

    private void openDonateDialog() {
        Context c = getContext();
        View dialogView = LayoutInflater.from(c).inflate(R.layout.dialog_donate, null);
        final Spinner amountSpinner = (Spinner) dialogView.findViewById(R.id.donate_amount_spinner);
        amountSpinner.setAdapter(new ArrayAdapter<String>(c, android.R.layout.simple_spinner_dropdown_item, donateTiers));
        Dialog donateDialog = new android.app.AlertDialog.Builder(c)
                .setIcon(R.drawable.ic_donate_green)
                .setTitle("Choose donation size")
                .setPositiveButton("Donate",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // do google donate
                                String selected = (String) amountSpinner.getSelectedItem();
                                if (selected.equals("$1"))
                                    selected = "donate_1";
                                else if (selected.equals("$5"))
                                    selected = "donate_5";
                                else if (selected.equals("$10"))
                                    selected = "donate_10";
                                else if (selected.equals("$20"))
                                    selected = "donate_20";
                                final String fSel = selected;
                                mCheckout.whenReady(new Checkout.EmptyListener() {
                                    @Override
                                    public void onReady(BillingRequests requests) {
                                        requests.purchase(ProductTypes.IN_APP, fSel, null, mCheckout.getPurchaseFlow());
                                    }
                                });
                            }
                        }
                )
                .setNegativeButton("Cancel", null)
                .setView(dialogView)
                .create();
        donateDialog.show();
    }

    private Billing mBilling;
    private ActivityCheckout mCheckout;
    private Inventory mInventory;

    Spinner navigationServerSpinner;

    private void openActivityWithDelay(final Class<?> activity, final int delay) {
        navigationServerSpinner.postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(getContext(), activity));
            }
        }, delay);
    }

    private void initCheckout() {
        mBilling = new Billing(getContext(), new Billing.DefaultConfiguration() {
            @Override
            public String getPublicKey() {
                return "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiIqX4BcfArotbMxUF5iiq59rAxpAr51IH+AnEITfLehYefQVSCuCXsrierzStuu6xgdzVaLvd/pHTTWRvMGMsVIfwn7Tkqf+FoIcPsoKyVs1psYHgpcbbYdBEaLZ8yrj895toYMeoGAZUqd2rEsb6T3JxLIo2oqH97sM5jAFFmgitEC/nSWNtIUqshI6IWX2smgc7uDRLQGZjsY/qwxcbVEa7yhgWN5ct/YfARKKR4qu5s0BirMIr+4NFgRLeZh5tyEuq3Bmlvpv1e4xxu2wsJmxsjV7IKU6yS1MFMvI2LntM+umvNR9jJpYkfNJ3UYG7CqVqC97T2hXmgqPk/0jhQIDAQAB";
            }
        });
        mCheckout = Checkout.forActivity(getActivity(), mBilling);

        mCheckout.start();
        mCheckout.createPurchaseFlow(new PurchaseListener());

        mInventory = mCheckout.makeInventory();
        mInventory.load(Inventory.Request.create()
                .loadAllPurchases()
                .loadSkus(ProductTypes.IN_APP, "donate_1"), new InventoryCallback());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View inflated = inflater.inflate(R.layout.fragment_navigation_list, container, false);

        initCheckout();

        navigationServerSpinner = (Spinner) inflated.findViewById(R.id.navigation_server_spinner);
        navigationServerSpinner.setAdapter(new SavedServerArrayAdapter(getContext(), WifiMouseApplication.savedServers));
        navigationServerSpinner.setSelection(WifiMouseApplication.getSelectedServerPosition(), false);
        navigationServerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Object item = parent.getItemAtPosition(position);
                if((item instanceof WifiMouseApplication.KnownServer) == false)
                    return;
                WifiMouseApplication.KnownServer server = (WifiMouseApplication.KnownServer) item;
                WifiMouseApplication.setSelectedServer(server);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        navigationServerSpinner.post(new Runnable() {
            @Override
            public void run() {
                if(navigationServerSpinner == null)
                    return;
                // Update list of saved servers
                SavedServerArrayAdapter adapter = (SavedServerArrayAdapter) navigationServerSpinner.getAdapter();
                adapter.notifyDataSetChanged();
                // Make sure selection is correct still
                if(navigationServerSpinner.getSelectedItemPosition() != WifiMouseApplication.getSelectedServerPosition())
                    navigationServerSpinner.setSelection(WifiMouseApplication.getSelectedServerPosition(), false);
                navigationServerSpinner.postDelayed(this, 1000);
            }
        });

        inflated.findViewById(R.id.navigation_remotes_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getActivity() instanceof NavigationDrawerToggler)
                    ((NavigationDrawerToggler)getActivity()).toggleNavigationDrawer();
                if(getActivity() instanceof RemoteListActivity == false)
                    openActivityWithDelay(RemoteListActivity.class, 200); // Briefly wait for drawer to close
            }
        });
        inflated.findViewById(R.id.navigation_servers_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getActivity() instanceof NavigationDrawerToggler)
                    ((NavigationDrawerToggler)getActivity()).toggleNavigationDrawer();
                if(getActivity() instanceof ServerChooserActivity == false)
                    openActivityWithDelay(ServerChooserActivity.class, 200);
            }
        });
        inflated.findViewById(R.id.navigation_tutorial_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getActivity() instanceof NavigationDrawerToggler)
                    ((NavigationDrawerToggler)getActivity()).toggleNavigationDrawer();
                openActivityWithDelay(TutorialActivity.class, 200);
            }
        });

        inflated.findViewById(R.id.navigation_rate_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=vivid.designs.wifimouse")));
            }
        });
        inflated.findViewById(R.id.navigation_share_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Check out WifiMouse for Android! Free with lots of great features! https://play.google.com/store/apps/details?id=vivid.designs.wifimouse");
                getContext().startActivity(Intent.createChooser(shareIntent, "Share via"));
            }
        });
        inflated.findViewById(R.id.navigation_donate_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDonateDialog();
            }
        });

        inflated.findViewById(R.id.navigation_exit_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Activity activity = getActivity();
                if(activity == null)
                    return;
                if(activity instanceof NavigationDrawerToggler)
                    ((NavigationDrawerToggler)activity).toggleNavigationDrawer();
                inflated.postDelayed(new Runnable() { // Wait briefly for drawer to close
                    @Override
                    public void run() {
                        activity.getApplicationContext().stopService(new Intent(getContext(), NetworkService.class));
                        activity.moveTaskToBack(true);
                        activity.finish();
                    }
                }, 250);
            }
        });

        return inflated;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        mCheckout.stop();
        super.onDestroyView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCheckout.onActivityResult(requestCode, resultCode, data);
    }

    class SavedServerArrayAdapter extends ArrayAdapter<WifiMouseApplication.KnownServer> {

        public SavedServerArrayAdapter(@NonNull Context context, List<WifiMouseApplication.KnownServer> servers) {
            super(context, 0, servers);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getRowView(convertView, position);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if(position < 0 || position >= WifiMouseApplication.savedServers.size()) {
                View v = new View(getContext());
                v.setVisibility(View.GONE);
                return v;
            }
            WifiMouseApplication.KnownServer server = WifiMouseApplication.savedServers.get(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_element_navigation, parent, false);
            }

            TextView serverName = (TextView) convertView.findViewById(R.id.server_element_name);
            serverName.setText(server.name);

            ImageView icon = (ImageView) convertView.findViewById(R.id.server_element_icon);
            icon.setImageResource(server.bluetooth? R.drawable.ic_bluetooth:R.drawable.ic_wifi);

            return convertView;
        }

        private View getRowView(View convertView, int position) {
            View dropDown = getDropDownView(position, convertView, null);

            TextView serverName = (TextView) dropDown.findViewById(R.id.server_element_name);
            serverName.setTextColor(Color.argb(200, 255, 255, 255));

            View circleBg = dropDown.findViewById(R.id.circle_bg);
            circleBg.setVisibility(View.VISIBLE);

            return dropDown;
        }
    }

    interface NavigationDrawerToggler {
        void toggleNavigationDrawer();
    }
}
