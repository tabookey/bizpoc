package com.tabookey.bizpoc;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class SendFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.send_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Button continueButton = view.findViewById(R.id.continueButton);
        continueButton.setOnClickListener(v->{
            ConfirmFragment cf = new ConfirmFragment();
            FragmentActivity activity = getActivity();
            if (activity == null){
                return;
            }
            activity.getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, cf).addToBackStack(null).commit();
        });
    }
}
