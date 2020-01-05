package com.silverintegral.easyphotoshare;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.HashMap;


public class QrViewFragment extends Fragment {
	private View m_activity_qr_view = null;
	private View m_fragment_qr_view = null;
	private View m_fragment_qr_edit = null;
	private QrActivity m_parentActivity = null;

	private OnFragmentInteractionListener mListener;

	private String m_ip = null;
	private int m_port = 0;
	private String m_ssid = "";
	private String m_pass = "";


	public QrViewFragment(QrActivity parent) {
		m_parentActivity = parent;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments() != null) {
			m_ip = getArguments().getString("HOST_IP", "");
			m_port = getArguments().getInt("HOST_PORT", 0);
			m_ssid = getArguments().getString("AP_SSID", "");
			m_pass = getArguments().getString("AP_PASS", "");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		//super.onCreateView(inflater, container, savedInstanceState);

		m_activity_qr_view = inflater.inflate(R.layout.activity_qr, null);
		m_fragment_qr_view = inflater.inflate(R.layout.fragment_qr_view, null);
		m_fragment_qr_edit = inflater.inflate(R.layout.fragment_qr_edit, null);

		return inflater.inflate(R.layout.fragment_qr_view, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		RefreshUI();
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);

		if (context instanceof OnFragmentInteractionListener) {
			mListener = (OnFragmentInteractionListener) context;
		} else {
			throw new RuntimeException(context.toString()
					+ " must implement OnFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}

	public void setWifiInfo(String ssid, String pass) {
		m_ssid = ssid;
		m_pass = pass;
	}



	private void RefreshUI() {
		try {
			BarcodeEncoder barcodeEncoder = new BarcodeEncoder();

			if (m_ip.length() > 0) {
				String barcodeUrl = "http://" + m_ip + ":" + m_port;

				Bitmap bitmap1 = barcodeEncoder.encodeBitmap(barcodeUrl, BarcodeFormat.QR_CODE, 1000, 1000);
				ImageView imageQr1 = getActivity().findViewById(R.id.qr_1);
				imageQr1.setImageBitmap(bitmap1);
				getActivity().findViewById(R.id.txt_1).setVisibility(View.VISIBLE);
				getActivity().findViewById(R.id.qr_1).setVisibility(View.VISIBLE);
			} else {
				getActivity().findViewById(R.id.txt_1).setVisibility(View.INVISIBLE);
				getActivity().findViewById(R.id.qr_1).setVisibility(View.INVISIBLE);
			}

			if (m_ssid.length() > 0 && m_pass.length() > 0) {
				String barcodeApn = "WIFI:T:WPA;S:" + m_ssid + ";P:" + m_pass + ";;";

				Bitmap bitmap2 = barcodeEncoder.encodeBitmap(barcodeApn, BarcodeFormat.QR_CODE, 1000, 1000);
				ImageView imageQr2 = getActivity().findViewById(R.id.qr_2);
				imageQr2.setImageBitmap(bitmap2);
				getActivity().findViewById(R.id.txt_2).setVisibility(View.VISIBLE);
				getActivity().findViewById(R.id.qr_2).setVisibility(View.VISIBLE);
			} else {
				getActivity().findViewById(R.id.txt_2).setVisibility(View.INVISIBLE);
				getActivity().findViewById(R.id.qr_2).setVisibility(View.INVISIBLE);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction in this fragment to be communicated
	 * to the activity and potentially other fragments contained in that
	 * activity.
	 * <p>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html"
	 * >Communicating with Other Fragments</a> for more information.
	 */
	public interface OnFragmentInteractionListener {
		// TODO: Update argument type and name
		void onFragmentInteraction(Uri uri);
	}
}
