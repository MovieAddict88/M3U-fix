package com.cinecraze.free;

import android.os.Bundle;
import android.widget.Toast;

/**
 * Simple test activity to demonstrate that the fragment implementation is working.
 * This can be used to test the fragment-based navigation before fully switching over.
 * 
 * To test this, you can temporarily change the AndroidManifest.xml to launch this activity instead:
 * android:name=".FragmentTestActivity"
 */
public class FragmentTestActivity extends FragmentMainActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Show a toast to confirm this is the fragment-based version
        Toast.makeText(this, "Fragment-based CineCraze loaded successfully!", Toast.LENGTH_LONG).show();
    }
}