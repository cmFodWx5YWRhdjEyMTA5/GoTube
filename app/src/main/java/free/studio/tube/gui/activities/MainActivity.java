/*
 * GoTube
 * Copyright (C) 2015  Ramon Mifsud
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package free.studio.tube.gui.activities;

import android.Manifest;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.admodule.AdModule;
import com.admodule.adfb.IFacebookAd;
import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.SearchSuggestionsAdapter;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;
import com.facebook.ads.NativeAd;

import org.json.JSONArray;
import org.mozilla.javascript.tools.jsc.Main;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import free.rm.gotube.R;
import free.studio.tube.app.AdsID;
import free.studio.tube.app.GoTubeApp;
import free.studio.tube.businessobjects.SreentUtils;
import free.studio.tube.businessobjects.TubeSearchSuggistion;
import free.studio.tube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.studio.tube.businessobjects.YouTube.POJOs.YouTubePlaylist;
import free.studio.tube.businessobjects.db.DownloadedVideosDb;
import free.studio.tube.gui.businessobjects.MainActivityListener;
import free.studio.tube.gui.businessobjects.YouTubePlayer;
import free.studio.tube.gui.businessobjects.adapters.RecyclerViewAdapterEx;
import free.studio.tube.gui.businessobjects.adapters.SubsAdapter;
import free.studio.tube.gui.fragments.ChannelBrowserFragment;
import free.studio.tube.gui.fragments.MainFragment;
import free.studio.tube.gui.fragments.PlaylistVideosFragment;
import free.studio.tube.gui.fragments.SearchVideoGridFragment;
import free.studio.tube.gui.fragments.VideosGridFragment;
import kr.co.namee.permissiongen.PermissionFail;
import kr.co.namee.permissiongen.PermissionGen;
import kr.co.namee.permissiongen.PermissionSuccess;
import q.rorbin.badgeview.Badge;
import q.rorbin.badgeview.QBadgeView;

/**
 * Main activity (launcher).  This activity holds {@link VideosGridFragment}.
 */
public class MainActivity extends AppCompatActivity implements MainActivityListener {
	@BindView(R.id.fragment_container)
	protected FrameLayout fragmentContainer;

	private MainFragment mainFragment;
	private SearchVideoGridFragment searchVideoGridFragment;
	private ChannelBrowserFragment channelBrowserFragment;
	// Fragment that shows Videos from a specific Playlist
	private PlaylistVideosFragment playlistVideosFragment;

	/** Set to true of the UpdatesCheckerTask has run; false otherwise. */
	private static boolean updatesCheckerTaskRan = false;
	public static final String ACTION_VIEW_CHANNEL = "MainActivity.ViewChannel";
	public static final String ACTION_VIEW_FEED = "MainActivity.ViewFeed";
	private static final String MAIN_FRAGMENT   = "MainActivity.MainFragment";
	private static final String SEARCH_FRAGMENT = "MainActivity.SearchFragment";
	public static final String CHANNEL_BROWSER_FRAGMENT = "MainActivity.ChannelBrowserFragment";
	public static final String PLAYLIST_VIDEOS_FRAGMENT = "MainActivity.PlaylistVideosFragment";

	private boolean dontAddToBackStack = false;


	@Override public void onRequestPermissionsResult(int requestCode, String[] permissions,
													 int[] grantResults) {
		PermissionGen.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
	}

	private RecyclerView subsListView = null;
	private SubsAdapter subsAdapter = null;
	private DrawerLayout subsDrawerLayout = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// check for updates (one time only)
//		if (!updatesCheckerTaskRan) {
//			new UpdatesCheckerTask(this, false).executeInParallel();
//			updatesCheckerTaskRan = true;
//		}

		GoTubeApp.setFeedUpdateInterval();
		// Delete any missing downloaded videos
		new DownloadedVideosDb.RemoveMissingVideosTask().executeInParallel();

		setContentView(R.layout.activity_main);
		ButterKnife.bind(this);

		if(fragmentContainer != null) {
			if(savedInstanceState != null) {
				mainFragment = (MainFragment)getSupportFragmentManager().getFragment(savedInstanceState, MAIN_FRAGMENT);
				searchVideoGridFragment = (SearchVideoGridFragment) getSupportFragmentManager().getFragment(savedInstanceState, SEARCH_FRAGMENT);
				channelBrowserFragment = (ChannelBrowserFragment) getSupportFragmentManager().getFragment(savedInstanceState, CHANNEL_BROWSER_FRAGMENT);
				playlistVideosFragment = (PlaylistVideosFragment) getSupportFragmentManager().getFragment(savedInstanceState, PLAYLIST_VIDEOS_FRAGMENT);

			}

			// If this Activity was called to view a particular channel, display that channel via ChannelBrowserFragment, instead of MainFragment
			String action = getIntent().getAction();
			if(ACTION_VIEW_CHANNEL.equals(action)) {
				dontAddToBackStack = true;
				YouTubeChannel channel = (YouTubeChannel) getIntent().getSerializableExtra(ChannelBrowserFragment.CHANNEL_OBJ);
				onChannelClick(channel);
			} else {
				if(mainFragment == null) {
					mainFragment = new MainFragment();
					// If we're coming here via a click on the Notification that new videos for subscribed channels have been found, make sure to
					// select the Feed tab.
					if(action != null && action.equals(ACTION_VIEW_FEED)) {
						Bundle args = new Bundle();
						args.putBoolean(MainFragment.SHOULD_SELECTED_FEED_TAB, true);
						mainFragment.setArguments(args);
					}
					getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, mainFragment).commit();
				}
			}
		}

		PermissionGen.with(this)
				.addRequestCode(100)
				.permissions(
						Manifest.permission.WRITE_EXTERNAL_STORAGE,
						Manifest.permission.ACCESS_COARSE_LOCATION,
						Manifest.permission.ACCESS_FINE_LOCATION,
						Manifest.permission.READ_PHONE_STATE)
				.request();

		subsDrawerLayout = findViewById(R.id.subs_drawer_layout);

		subsListView = findViewById(R.id.subs_drawer);
		if (subsAdapter == null) {
			subsAdapter = SubsAdapter.get(this, findViewById(R.id.subs_drawer_progress_bar));
		} else {
			subsAdapter.setContext(this);
		}
		subsAdapter.setListener(this);

		subsListView.setLayoutManager(new LinearLayoutManager(this));
		subsListView.setAdapter(subsAdapter);

		initSearchView();

	}

	private FloatingSearchView mSearchView;
	private boolean isSearched;

	private void initSearchView() {
		mSearchView = findViewById(R.id.floating_search_view);
		mSearchView.attachNavigationDrawerToMenuButton(subsDrawerLayout);

		mSearchView.setSearchHint(getString(R.string.app_name));

		initSearchRedPoint(mSearchView);

		mSearchView.setOnSearchListener(new FloatingSearchView.OnSearchListener() {
			@Override
			public void onSuggestionClicked(SearchSuggestion searchSuggestion) {
				onSearchAction(searchSuggestion.getBody());
				mSearchView.clearSearchFocus();
				mSearchView.setSearchText(searchSuggestion.getBody());
			}

			@Override
			public void onSearchAction(String currentQuery) {
				isSearched = true;
				mSearchView.clearSuggestions();
				if (mSearchTask != null) {
					mSearchTask.cancel(true);
				}
				mSearchView.hideProgress();

				SearchVideoGridFragment searchVideoGridFragment = new SearchVideoGridFragment();
				Bundle bundle = new Bundle();
				bundle.putString(SearchVideoGridFragment.QUERY, currentQuery);
				searchVideoGridFragment.setArguments(bundle);

				FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
				transaction.replace(R.id.fragment_container, searchVideoGridFragment);
				transaction.addToBackStack(null);
				transaction.commitAllowingStateLoss();
			}
		});

		mSearchView.setOnBindSuggestionCallback(new SearchSuggestionsAdapter.OnBindSuggestionCallback() {
			@Override
			public void onBindSuggestion(View suggestionView, ImageView leftIcon, TextView textView, SearchSuggestion item, int itemPosition) {
				leftIcon.setImageResource(R.drawable.ic_search_606060_24dp);
				textView.setText(item.getBody());
			}
		});
		mSearchView.setOnQueryChangeListener(new FloatingSearchView.OnQueryChangeListener() {
			@Override
			public void onSearchTextChanged(String oldQuery, String newQuery) {
				Log.e("xx", ">> isSearched " + isSearched);
				if (isSearched) {
					isSearched = false;
					return;
				}
				if (!oldQuery.equals("") && newQuery.equals("")) {
					mSearchView.clearSuggestions();
				} else {
					searchSuggestions(newQuery);
				}
			}
		});
		mSearchView.setOnMenuItemClickListener(new FloatingSearchView.OnMenuItemClickListener() {
			@Override
			public void onActionMenuItemSelected(MenuItem item) {
				switch (item.getItemId()) {
					case R.id.menu_preferences:
						Intent i = new Intent(MainActivity.this, PreferencesActivity.class);
						startActivity(i);
						break;
					case R.id.menu_downloads:
						DownloadActivity.launch(getApplicationContext());
						break;
				}
			}
		});

	}

	Badge mRedMenuBadge;

	private void initSearchRedPoint(FloatingSearchView searchView) {
		mRedMenuBadge = new QBadgeView(getApplication())
				.bindTarget(searchView.findViewById(com.arlib.floatingsearchview.R.id.menu_view));
		mRedMenuBadge.setBadgeBackgroundColor(ContextCompat.getColor(getApplication(),
				R.color.colorPrimary));
		mRedMenuBadge.setBadgeGravity(Gravity.END | Gravity.TOP);
		mRedMenuBadge.setBadgeNumber(-1);
		mRedMenuBadge.setGravityOffset(6, true);
	}

	private AsyncTask mSearchTask;

	private void searchSuggestions(String newText) {
		if (mSearchTask != null) {
			mSearchTask.cancel(true);
		}

		mSearchTask = new AsyncTask<String, Void, List<SearchSuggestion>>() {

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				if (mSearchView != null) {
					mSearchView.showProgress();
				}
			}

			@Override
			protected List<SearchSuggestion> doInBackground(String... strings) {
				try {
					Log.v("suggion", "doInBackground suggistion");
					String query = strings[0];
					URL url = new URL("http://suggestqueries.google.com/complete/search?client=firefox&hl=fr&q=" + query);
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("GET");
					conn.connect();
					if (conn.getResponseCode() == 200) {
						InputStream is = conn.getInputStream();
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						byte[] buffer = new byte[1024];
						int len = 0;
						while ((len = is.read(buffer)) != -1) {
							baos.write(buffer, 0, len);
						}
						baos.close();
						is.close();
						byte[] byteArray = baos.toByteArray();
						String content = new String(byteArray);

						if (!TextUtils.isEmpty(content)) {
							JSONArray jsonArray = new JSONArray(content);
							for (int i = 0; i < jsonArray.length(); i++) {
								JSONArray jsonArray1 = jsonArray.optJSONArray(i);
								if (jsonArray1 != null) {
									ArrayList<SearchSuggestion> list = new ArrayList<>();
									for (int j = 0; j < jsonArray1.length(); j++) {
										String str = jsonArray1.getString(j);
										list.add(new TubeSearchSuggistion(str));
									}
									return list;
								}
							}
						}
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onCancelled() {
				super.onCancelled();
				mSearchView.hideProgress();
			}

			@Override
			protected void onPostExecute(List<SearchSuggestion> list) {
				super.onPostExecute(list);
				if (list != null && !isFinishing()) {
					mSearchView.swapSuggestions(list);
				}
				mSearchView.hideProgress();
			}
		}.executeOnExecutor(SreentUtils.sExecutorService, newText);
	}

	@PermissionSuccess(requestCode = 100)
	public void permissionsSuccess(){

	}

	@PermissionFail(requestCode = 100)
	private void permissionsFaild() {

	}


	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if(mainFragment != null)
			getSupportFragmentManager().putFragment(outState, MAIN_FRAGMENT, mainFragment);
		if(searchVideoGridFragment != null && searchVideoGridFragment.isVisible())
			getSupportFragmentManager().putFragment(outState, SEARCH_FRAGMENT, searchVideoGridFragment);
		if(channelBrowserFragment != null && channelBrowserFragment.isVisible())
			getSupportFragmentManager().putFragment(outState, CHANNEL_BROWSER_FRAGMENT, channelBrowserFragment);
		if(playlistVideosFragment != null && playlistVideosFragment.isVisible())
			getSupportFragmentManager().putFragment(outState, PLAYLIST_VIDEOS_FRAGMENT, playlistVideosFragment);
		super.onSaveInstanceState(outState);
	}


	@Override
	protected void onResume() {
		super.onResume();

		// Activity may be destroyed when the devices is rotated, so we need to make sure that the
		// channel play list is holding a reference to the activity being currently in use...
		if (channelBrowserFragment != null)
			channelBrowserFragment.getChannelPlaylistsFragment().setMainActivityListener(this);
	}


//	@Override
//	public boolean onCreateOptionsMenu(final Menu menu) {
////		MenuInflater inflater = getMenuInflater();
////		inflater.inflate(R.menu.main_activity_menu, menu);
////
////		// setup the SearchView (actionbar)
////		final MenuItem searchItem = menu.findItem(R.id.menu_search);
////		final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
////
////		if (searchView == null) {
////			return true;
////		}
////
////		searchView.setQueryHint(getString(R.string.search_videos));
////		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
////			@Override
////			public boolean onQueryTextChange(String newText) {
////				return false;
////			}
////
////			@Override
////			public boolean onQueryTextSubmit(String query) {
////				// hide the keyboard
////				searchView.clearFocus();
////
////				// open SearchVideoGridFragment and display the results
////				searchVideoGridFragment = new SearchVideoGridFragment();
////				Bundle bundle = new Bundle();
////				bundle.putString(SearchVideoGridFragment.QUERY, query);
////				searchVideoGridFragment.setArguments(bundle);
////				switchToFragment(searchVideoGridFragment);
////
////				return true;
////			}
////		});
//
//		return true;
//	}


//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//		switch (item.getItemId()) {
//			case R.id.menu_preferences:
//				Intent i = new Intent(this, PreferencesActivity.class);
//				startActivity(i);
//				return true;
//			case R.id.menu_enter_video_url:
//				displayEnterVideoUrlDialog();
//				return true;
//			case android.R.id.home:
//				if(mainFragment == null || !mainFragment.isVisible()) {
//					onBackPressed();
//					return true;
//				}
//		}
//
//		return super.onOptionsItemSelected(item);
//	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * Display the Enter Video URL dialog.
	 */
	private void displayEnterVideoUrlDialog() {
		final AlertDialog alertDialog = new AlertDialog.Builder(this)
			.setView(R.layout.dialog_enter_video_url)
			.setTitle(R.string.enter_video_url)
			.setPositiveButton(R.string.play, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// get the inputted URL string
					final String videoUrl = ((EditText)((AlertDialog) dialog).findViewById(R.id.dialog_url_edittext)).getText().toString();

					// play the video
					YouTubePlayer.launch(videoUrl, MainActivity.this);
				}
			})
			.setNegativeButton(R.string.cancel, null)
			.show();

		// paste whatever there is in the clipboard (hopefully it is a video url)
		((EditText) alertDialog.findViewById(R.id.dialog_url_edittext)).setText(getClipboardItem());

		// clear URL edittext button
		alertDialog.findViewById(R.id.dialog_url_clear_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				((EditText) alertDialog.findViewById(R.id.dialog_url_edittext)).setText("");
			}
		});
	}


	/**
	 * Return the last item stored in the clipboard.
	 *
	 * @return	{@link String}
	 */
	private String getClipboardItem() {
		String item = "";

		ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		if (clipboard.hasPrimaryClip()) {
			android.content.ClipDescription description = clipboard.getPrimaryClipDescription();
			android.content.ClipData data = clipboard.getPrimaryClip();
			if (data != null && description != null && description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))
				item = String.valueOf(data.getItemAt(0).getText());
		}

		return item;
	}


	@Override
	public void onBackPressed() {
		if (mainFragment != null  &&  mainFragment.isVisible()) {
			// If the Subscriptions Drawer is open, close it instead of minimizing the app.
			if(subsDrawerLayout.isDrawerOpen(GravityCompat.START)) {
				subsDrawerLayout.closeDrawer(GravityCompat.START);
			} else {
				// On Android, when the user presses back button, the Activity is destroyed and will be
				// recreated when the user relaunches the app.
				// We do not want that behaviour, instead then the back button is pressed, the app will
				// be **minimized**.
				Intent startMain = new Intent(Intent.ACTION_MAIN);
				startMain.addCategory(Intent.CATEGORY_HOME);
				startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(startMain);
			}
		} else {
			super.onBackPressed();
		}
	}


	public void switchToFragment(Fragment fragment) {
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

		transaction.replace(R.id.fragment_container, fragment);
		if(!dontAddToBackStack)
			transaction.addToBackStack(null);
		else {
			dontAddToBackStack = false;
		}
		transaction.commitAllowingStateLoss();
	}



	@Override
	public void onChannelClick(YouTubeChannel channel) {
		Bundle args = new Bundle();
		args.putSerializable(ChannelBrowserFragment.CHANNEL_OBJ, channel);
		switchToChannelBrowserFragment(args);
	}

	@Override
	public void onChannelClick(String channelId) {
		Bundle args = new Bundle();
		args.putString(ChannelBrowserFragment.CHANNEL_ID, channelId);
		switchToChannelBrowserFragment(args);
	}

	private void switchToChannelBrowserFragment(Bundle args) {
		channelBrowserFragment = new ChannelBrowserFragment();
		channelBrowserFragment.getChannelPlaylistsFragment().setMainActivityListener(this);
		channelBrowserFragment.setArguments(args);
		switchToFragment(channelBrowserFragment);
	}

	@Override
	public void onPlaylistClick(YouTubePlaylist playlist) {
		playlistVideosFragment = new PlaylistVideosFragment();
		Bundle args = new Bundle();
		args.putSerializable(PlaylistVideosFragment.PLAYLIST_OBJ, playlist);
		playlistVideosFragment.setArguments(args);
		switchToFragment(playlistVideosFragment);
	}

}
