package mil.nga.mgrs.app.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import mil.nga.mgrs.app.R;


public class ProgressBarFragment extends Fragment {

    Fragment fragment;
    FragmentManager fragmentManager;
    FragmentTransaction fragmentTransaction;
    EditText emailTxtInput, pwdTxtInput;
    Button loginBtn, toRegisterBtn;
    View progressBarView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        initalizeFragment(inflater, container);
        return progressBarView;
    }

    public void initalizeFragment(@NonNull LayoutInflater inflater, @Nullable ViewGroup container){
        progressBarView = inflater.inflate(R.layout.progressbar, container, false);
    }

    public void changeFragment(Fragment fragmentToChange) {
        fragment = fragmentToChange;
        fragmentManager = getFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer, fragment);
        fragmentTransaction.commit();
    }
}