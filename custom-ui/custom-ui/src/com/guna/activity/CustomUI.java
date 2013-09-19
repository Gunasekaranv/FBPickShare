package com.guna.activity;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

import org.brickred.socialauth.Photo;
import org.brickred.socialauth.Profile;
import org.brickred.socialauth.android.DialogListener;
import org.brickred.socialauth.android.SocialAuthAdapter;
import org.brickred.socialauth.android.SocialAuthError;
import org.brickred.socialauth.android.SocialAuthListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.guna.customadapter.CustomAdapter;
import com.guna.customadapter.R;

// Please see strings.xml for list values

public class CustomUI extends Activity {

	// SocialAuth Components
	SocialAuthAdapter adapter;
	Profile profileMap;
	List<Photo> photosList;

	// Android Components
	ListView listview;
	AlertDialog dialog;
	TextView title;
	ProgressDialog mDialog;

	// Variables
	boolean status;
	String providerName;
	public static int pos;
	private static final int SELECT_PHOTO = 100;
	public static Bitmap bitmap;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Adapter initialization
		adapter = new SocialAuthAdapter(new ResponseListener());

		// Set title
		title = (TextView) findViewById(R.id.textview);
		title.setText(R.string.app_name);

		listview = (ListView) findViewById(R.id.listview);
		listview.setAdapter(new CustomAdapter(this, adapter));
	}

	// To receive the response after authentication
	private final class ResponseListener implements DialogListener {

		@Override
		public void onComplete(Bundle values) {

			Log.d("Custom-UI", "Successful");

			// Changing Sign In Text to Sign Out
			View v = listview.getChildAt(pos
					- listview.getFirstVisiblePosition());
			TextView pText = (TextView) v.findViewById(R.id.signstatus);
			pText.setText("Sign Out");

			// Get the provider
			providerName = values.getString(SocialAuthAdapter.PROVIDER);
			Log.d("Custom-UI", "providername = " + providerName);

			Toast.makeText(CustomUI.this, providerName + " connected",
					Toast.LENGTH_SHORT).show();

			int res = getResources().getIdentifier(providerName + "_array",
					"array", CustomUI.this.getPackageName());

			AlertDialog.Builder builder = new AlertDialog.Builder(CustomUI.this);
			builder.setTitle("Select Options");
			builder.setCancelable(true);
			builder.setIcon(android.R.drawable.ic_menu_more);

			mDialog = new ProgressDialog(CustomUI.this);
			mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			mDialog.setMessage("Loading...");

			builder.setSingleChoiceItems(
					new DialogAdapter(CustomUI.this, R.layout.provider_options,
							getResources().getStringArray(res)), 0,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int item) {

							Events(item, providerName);
							dialog.dismiss();
						}
					});
			dialog = builder.create();
			dialog.show();

		}

		@Override
		public void onError(SocialAuthError error) {
			Log.d("Custom-UI", "Error");
			error.printStackTrace();
		}

		@Override
		public void onCancel() {
			Log.d("Custom-UI", "Cancelled");
		}

		@Override
		public void onBack() {
			Log.d("Custom-UI", "Dialog Closed by pressing Back Key");

		}
	}

	// Method to handle events of providers
	public void Events(int position, final String provider) {

		switch (position) {

		case 4: {
			// Upload Image for Facebook and Twitter

			if (provider.equalsIgnoreCase("facebook")
					|| provider.equalsIgnoreCase("twitter")) {

				// Code to Post Message for all providers
				final Dialog imgDialog = new Dialog(CustomUI.this);
				imgDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				imgDialog.setContentView(R.layout.dialog);
				imgDialog.setCancelable(true);

				TextView dialogTitle = (TextView) imgDialog
						.findViewById(R.id.dialogTitle);
				dialogTitle.setText("Share Image");
				final EditText edit = (EditText) imgDialog
						.findViewById(R.id.editTxt);
				Button update = (Button) imgDialog.findViewById(R.id.update);
				update.setVisibility(View.INVISIBLE);
				Button getImage = (Button) imgDialog
						.findViewById(R.id.loadImage);
				getImage.setVisibility(View.VISIBLE);
				imgDialog.show();

				getImage.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {

						// Taking image from phone gallery
						Intent photoPickerIntent = new Intent(
								Intent.ACTION_PICK);
						photoPickerIntent.setType("image/*");
						startActivityForResult(photoPickerIntent, SELECT_PHOTO);

						if (bitmap != null) {
							mDialog.show();
							try {
								adapter.uploadImageAsync(edit.getText()
										.toString(), "icon.png", bitmap, 0,
										new UploadImageListener());
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						imgDialog.dismiss();
					}
				});

			}
			break;
		}

		}

	}

	// To get status of image upload after authentication
	private final class UploadImageListener implements
			SocialAuthListener<Integer> {

		@Override
		public void onExecute(String provider, Integer t) {
			mDialog.dismiss();
			Integer status = t;
			Log.d("Custom-UI", String.valueOf(status));
			Toast.makeText(CustomUI.this, "Image Uploaded", Toast.LENGTH_SHORT)
					.show();
		}

		@Override
		public void onError(SocialAuthError e) {

		}
	}

	// To receive the feed response after authentication

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent imageReturnedIntent) {
		super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

		switch (requestCode) {
		case SELECT_PHOTO:
			if (resultCode == RESULT_OK) {
				Uri selectedImage = imageReturnedIntent.getData();
				InputStream imageStream;
				try {
					imageStream = getContentResolver().openInputStream(
							selectedImage);
					bitmap = BitmapFactory.decodeStream(imageStream);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}

			}
		}
	}

	/**
	 * CustomAdapter for showing List. On clicking any item , it calls
	 * authorize() method to authenticate provider
	 */

	public class DialogAdapter extends BaseAdapter {
		// Android Components
		private final LayoutInflater mInflater;
		private final Context ctx;
		private Drawable mIcon;
		String[] drawables;
		String[] options;

		public DialogAdapter(Context context, int textViewResourceId,
				String[] providers) {
			// Cache the LayoutInflate to avoid asking for a new one each time.
			ctx = context;
			mInflater = LayoutInflater.from(ctx);
			options = providers;
		}

		/**
		 * The number of items in the list is determined by the number of
		 * speeches in our array.
		 */
		@Override
		public int getCount() {
			return options.length;
		}

		/**
		 * Since the data comes from an array, just returning the index is
		 * sufficent to get at the data. If we were using a more complex data
		 * structure, we would return whatever object represents one row in the
		 * list.
		 */
		@Override
		public Object getItem(int position) {
			return position;
		}

		/**
		 * Use the array index as a unique id.
		 */
		@Override
		public long getItemId(int position) {
			return position;
		}

		/**
		 * Make a view to hold each row.
		 * 
		 * @see android.widget.ListAdapter#getView(int, android.view.View,
		 *      android.view.ViewGroup)
		 */
		@Override
		public View getView(final int position, View convertView,
				ViewGroup parent) {
			// A ViewHolder keeps references to children views to avoid
			// unneccessary
			// calls to findViewById() on each row.
			ViewHolder holder;

			// When convertView is not null, we can reuse it directly, there is
			// no
			// need to reinflate it. We only inflate a new View when the
			// convertView
			// supplied by ListView is null.
			if (convertView == null) {
				convertView = mInflater
						.inflate(R.layout.provider_options, null);

				// Creates a ViewHolder and store references to the two children
				// views
				// we want to bind data to.
				holder = new ViewHolder();
				holder.text = (TextView) convertView
						.findViewById(R.id.providerText);
				holder.icon = (ImageView) convertView
						.findViewById(R.id.provider);

				convertView.setTag(holder);
			} else {
				// Get the ViewHolder back to get fast access to the TextView
				// and the ImageView.
				holder = (ViewHolder) convertView.getTag();
			}

			String drawables[] = ctx.getResources().getStringArray(
					R.array.drawable_array);

			mIcon = ctx.getResources().getDrawable(
					ctx.getResources().getIdentifier(drawables[position],
							"drawable", ctx.getPackageName()));

			// Bind the data efficiently with the holder
			holder.text.setText(options[position]);
			if (options[position].equalsIgnoreCase("career"))
				holder.icon.setImageResource(R.drawable.career);
			else
				holder.icon.setImageDrawable(mIcon);

			return convertView;
		}

		class ViewHolder {
			TextView text;
			ImageView icon;
		}
	}
}