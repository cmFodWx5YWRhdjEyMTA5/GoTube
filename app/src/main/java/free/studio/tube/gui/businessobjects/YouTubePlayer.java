/*
 * GoTube
 * Copyright (C) 2016  Ramon Mifsud
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

package free.studio.tube.gui.businessobjects;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import free.rm.gotube.R;
import free.studio.tube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.studio.tube.gui.activities.YouTubePlayerActivity;
import free.studio.tube.gui.fragments.YouTubePlayerFragment;
import free.studio.tube.gui.player.TubePlayerActivity;

/**
 * Launches YouTube player.
 */
public class YouTubePlayer {

	private static final String TAG = YouTubePlayer.class.getSimpleName();

	/**
	 * Launches the custom-made YouTube player so that the user can view the selected video.
	 *
	 * @param youTubeVideo Video to be viewed.
	 */
	public static void launch(YouTubeVideo youTubeVideo, Context context) {
		Intent i = new Intent(context, YouTubePlayerActivity.class);
		i.putExtra(YouTubePlayerFragment.YOUTUBE_VIDEO_OBJ, youTubeVideo);
		context.startActivity(i);
	}

	public static void launch(YouTubeVideo youTubeVideo, Activity activity) {
		Intent i = new Intent(activity, YouTubePlayerActivity.class);
		i.putExtra(YouTubePlayerFragment.YOUTUBE_VIDEO_OBJ, youTubeVideo);
		activity.startActivity(i);
//		activity.overridePendingTransition(R.anim.slide_bottom_in, 0);
	}

	public static void launch(Context context, YouTubeVideo youTubeVideo) {
//		Intent i = new Intent(context, YouTubePlayerActivity.class);
//		i.putExtra(YouTubePlayerFragment.YOUTUBE_VIDEO_OBJ, youTubeVideo);
//		context.startActivity(i);
//		activity.overridePendingTransition(R.anim.slide_bottom_in, 0);
		TubePlayerActivity.launch(context, youTubeVideo);
	}


	/**
	 * Launches the custom-made YouTube player so that the user can view the selected video.
	 *
	 * @param videoUrl URL of the video to be watched.
	 */
	public static void launch(String videoUrl, Context context) {
		Intent i = new Intent(context, YouTubePlayerActivity.class);
		i.setAction(Intent.ACTION_VIEW);
		i.setData(Uri.parse(videoUrl));
		context.startActivity(i);
	}

	public static void launch(String videoUrl, Activity activity) {
		Intent i = new Intent(activity, YouTubePlayerActivity.class);
		i.setAction(Intent.ACTION_VIEW);
		i.setData(Uri.parse(videoUrl));
		activity.startActivity(i);
//		activity.overridePendingTransition(R.anim.slide_bottom_in, 0);
	}

}