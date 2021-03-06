/*
 * GoTube
 * Copyright (C) 2018  Ramon Mifsud
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

package com.tubeplayer.player.business.db.Tasks;

import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.List;

import com.tubeplayer.player.business.AsyncTaskParallel;
import com.tubeplayer.player.business.youtube.bean.YTubeChannel;
import com.tubeplayer.player.gui.businessobjects.adapters.SubsAdapter;
import com.tube.playtube.R;
import com.tubeplayer.player.business.db.SubscriptionsDb;

/**
 * Gets a list of channels (from the DB) that the user is subscribed to and then passes the channels
 * list to the given {@link SubsAdapter}.
 */
public class GetSubscribedChannelsTask extends AsyncTaskParallel<Void, Void, List<YTubeChannel>> {

	private SubsAdapter adapter;
	private View progressBar;

	private static final String TAG = GetSubscribedChannelsTask.class.getSimpleName();


	public GetSubscribedChannelsTask(SubsAdapter adapter, View progressBar) {
		this.adapter = adapter;
		this.progressBar = progressBar;
	}


	@Override
	protected void onPreExecute() {
		if (progressBar != null)
			progressBar.setVisibility(View.VISIBLE);
	}


	@Override
	protected List<YTubeChannel> doInBackground(Void... params) {
		List<YTubeChannel> subbedChannelsList = null;

		try {
			subbedChannelsList = SubscriptionsDb.getSubscriptionsDb().getSubscribedChannels();
		} catch (Throwable tr) {
			Log.e(TAG, "An error has occurred while getting subbed channels", tr);
		}

		return subbedChannelsList;
	}


	@Override
	protected void onPostExecute(List<YTubeChannel> subbedChannelsList) {
		if (progressBar != null) {
			progressBar.setVisibility(View.INVISIBLE);
			progressBar = null;
		}

		if (subbedChannelsList == null) {
			Toast.makeText(adapter.getContext(), R.string.error_get_subbed_channels, Toast.LENGTH_LONG).show();
		} else {
			adapter.appendList(subbedChannelsList);
		}

		// Notify the SubsAdapter that the subbed channel list has been retrieved and populated.  If
		// there is an error we still need to notify the adapter that the task has been completed
		// from this end...
		adapter.subsListRetrieved();
	}

}
