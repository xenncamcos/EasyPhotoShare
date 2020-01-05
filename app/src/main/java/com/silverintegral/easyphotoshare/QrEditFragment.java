package com.silverintegral.easyphotoshare;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;


public class QrEditFragment extends Fragment {
	private View m_activity_qr_view = null;
	private View m_fragment_qr_view = null;
	private View m_fragment_qr_edit = null;
	private QrActivity m_parentActivity = null;

	private OnFragmentInteractionListener mListener;

	private String m_ssid = "";
	private String m_pass = "";


	public QrEditFragment(QrActivity parent) {
		m_parentActivity = parent;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments() != null) {
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

		return inflater.inflate(R.layout.fragment_qr_edit, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		EditText ssid = getActivity().findViewById(R.id.qr_ssid);
		ssid.setText(m_ssid);
		EditText pass = getActivity().findViewById(R.id.qr_pass);
		pass.setText(m_pass);
	}


	@Override
	public void onPause() {
		super.onPause();

		EditText ssid = getActivity().findViewById(R.id.qr_ssid);
		EditText pass = getActivity().findViewById(R.id.qr_pass);

		m_ssid = ssid.getText().toString();
		m_pass = pass.getText().toString();

		m_parentActivity.setWifiInfo(m_ssid, m_pass);
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

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString("KEY", "value");
		super.onSaveInstanceState(outState);
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
