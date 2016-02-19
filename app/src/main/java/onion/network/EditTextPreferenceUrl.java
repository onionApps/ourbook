/*
 * Network.onion - fully distributed p2p social network using onion routing
 *
 * http://play.google.com/store/apps/details?id=onion.network
 * http://onionapps.github.io/Network.onion/
 * http://github.com/onionApps/Network.onion
 *
 * Author: http://github.com/onionApps - http://jkrnk73uid7p5thz.onion - bitcoin:1kGXfWx8PHZEVriCNkbP5hzD15HS4AyKf
 */

package onion.network;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

public class EditTextPreferenceUrl extends EditTextPreferenceEx {
/**/
    public EditTextPreferenceUrl(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditTextPreferenceUrl(Context context) {
        super(context);
    }

    @Override
    public EditText getEditText() {
        EditText ed = super.getEditText();
        ed.setImeOptions(ed.getImeOptions() | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        ed.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        return ed;
    }
}
