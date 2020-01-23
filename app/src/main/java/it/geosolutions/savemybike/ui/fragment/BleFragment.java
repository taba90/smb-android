package it.geosolutions.savemybike.ui.fragment;

import android.bluetooth.le.ScanSettings;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import it.geosolutions.savemybike.R;
import it.geosolutions.savemybike.data.server.RetrofitClient;
import it.geosolutions.savemybike.data.server.SMBRemoteServices;
import it.geosolutions.savemybike.model.Bike;
import it.geosolutions.savemybike.sensors.ResponseSensorsCallback;
import it.geosolutions.savemybike.sensors.bluetooth.BluetoothBleManager;
import it.geosolutions.savemybike.ui.activity.SaveMyBikeActivity;
import it.geosolutions.savemybike.ui.callback.IOnBackPressed;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BleFragment extends Fragment implements IOnBackPressed {

    public static final String TAG = "BleFragment";

    private BluetoothBleManager manager;

    @BindView(R.id.alarm_on)
    Button alarmOn;
    @BindView(R.id.alarm_off)
    Button alarmOf;
    @BindView(R.id.bike_ble_title)
    TextView bikeName;
    @BindView(R.id.loading_container_ble)
    View loading;
    @BindView(R.id.ble_bike_frame)
    LinearLayout bikeFrame;

    private String bleAddress;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        manager = BluetoothBleManager.get(ScanSettings.SCAN_MODE_LOW_POWER);
        manager.initialize(getActivity());
        final View view = inflater.inflate(R.layout.fragment_ble_bike, container, false);
        ButterKnife.bind(this, view);
        showLoading(true);
        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        if(!manager.isScanning() && bleAddress==null)
            manager.startScan();
    }

    @Override
    public void onPause() {
        super.onPause();
        manager.stopScan();
    }

    @Override
    public void onStop() {
        super.onStop();
        manager.stopScan();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        showFrame(false);
        manager.stopScan();
        manager.disconnect();
        manager.setActive(false);
    }

    public void issueRequest(String uuid) {
        RetrofitClient client = RetrofitClient.getInstance(getActivity());
        SMBRemoteServices portalServices = client.getPortalServices();
        client.performAuthenticatedCall(
                portalServices.getMyTaggedBike(uuid),
                new ResponseSensorsCallback(getContext()) {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        manager.stopScan();

                        String jsonBike = getJsonBikeFromResponseBody(response);
                        Bike bikeObj = jsonBike != null ? new Gson().fromJson(jsonBike, Bike.class) : null;
                        showLoading(false);
                        showFrame(true);

                        if (bikeObj != null) {
                            bikeName.setText(bikeObj.getName() != null ? bikeObj.getName() : bikeObj.getNickname());
                            bleAddress = uuid;
                            if (bleAddress != null) {
                                manager.connect(bleAddress);
                            }
                        }
                    }
                }
        );
    }

    public void showLoading(boolean show) {
        loading.setVisibility(show? View.VISIBLE:View.GONE);
    }

    public void showFrame(boolean show){
        bikeFrame.setVisibility(show?View.VISIBLE:View.GONE);
    }

    @OnClick(R.id.alarm_on)
    public void alarmOnClicked(){
        manager.setAlarmMode(3);
    }

    @OnClick(R.id.alarm_off)
    public void alarmOffClicked(){
        manager.setAlarmMode(0);
    }


    private void exit() {
        try {
            ((SaveMyBikeActivity) getActivity()).changeFragment(R.id.navigation_home);
        } catch (Exception e) {
            // and error happens when back button was pressed before save end.
            Log.e(TAG, "Error while exiting.", e);
        }
    }

    @Override
    public boolean onBackPressed() {
        exit();
        return true;
    }
}
