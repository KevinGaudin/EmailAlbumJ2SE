package com.kg.emailalbum.mobile;

// Need the following import to get access to the app resources, since this
// class is in a sub-package.
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
/**
 * <h3>Dialog Activity</h3>
 * 
 * <p>This demonstrates the how to write an activity that looks like 
 * a pop-up dialog.</p>
 */
public class AboutDialog extends Activity {
    /**
     * Initialization of the Activity after it is first created.  Must at least
     * call {@link android.app.Activity#setContentView setContentView()} to
     * describe what is to be displayed in the screen.
     */
    @Override
	protected void onCreate(Bundle savedInstanceState) {
        // Be sure to call the super class.
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        
        // See assets/res/any/layout/dialog_activity.xml for this
        // view layout definition, which is being set here as
        // the content of our screen.
        setContentView(R.layout.about_dialog);
//        TextView text = (TextView)findViewById(R.id.AboutText);
//        Spanned spanned = (Spanned) getText(R.string.about_text);
//        String htmlFormatString = Html.toHtml(spanned);
//        text.setText(htmlFormatString);
//        
        ImageButton donate = (ImageButton)findViewById(R.id.GoWebButton);
        donate.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getText(R.string.url_donate).toString()));
                startActivity(intent);
            }
        });
        
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, 
                android.R.drawable.ic_dialog_info);
    }
}
