package com.cinecraze.free;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.cinecraze.free.models.Entry;
import com.cinecraze.free.models.Season;
import com.cinecraze.free.models.Episode;
import com.cinecraze.free.models.Server;
import com.cinecraze.free.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Test activity to demonstrate the enhanced DetailsActivity functionality
 * This activity creates sample data and launches DetailsActivity
 */
public class TestDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_details);

        Button testMovieButton = findViewById(R.id.test_movie_button);
        Button testTVSeriesButton = findViewById(R.id.test_tv_series_button);

        testMovieButton.setOnClickListener(v -> testMovieDetails());
        testTVSeriesButton.setOnClickListener(v -> testTVSeriesDetails());
    }

    private void testMovieDetails() {
        try {
            Entry movieEntry = createSampleMovie();
            DetailsActivity.start(this, movieEntry);
        } catch (Exception e) {
            Toast.makeText(this, "Error testing movie: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void testTVSeriesDetails() {
        try {
            Entry tvSeriesEntry = createSampleTVSeries();
            DetailsActivity.start(this, tvSeriesEntry);
        } catch (Exception e) {
            Toast.makeText(this, "Error testing TV series: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Entry createSampleMovie() {
        Entry entry = new Entry();
        entry.setTitle("Sample Movie");
        entry.setDescription("This is a sample movie to test the enhanced DetailsActivity functionality.");
        entry.setMainCategory("Movie");
        entry.setSubCategory("Action");
        entry.setDuration("120 min");
        entry.setYear(2024);
        entry.setRating(8.5f);
        entry.setPoster("https://via.placeholder.com/300x450");
        entry.setThumbnail("https://via.placeholder.com/300x450");

        // Create sample servers
        List<Server> servers = new ArrayList<>();
        
        Server server1 = new Server();
        server1.setName("VidSrc.to");
        server1.setUrl("https://vidsrc.to/embed/movie/12345");
        servers.add(server1);
        
        Server server2 = new Server();
        server2.setName("VidJoy");
        server2.setUrl("https://vidjoy.pro/embed/movie/12345");
        servers.add(server2);
        
        Server server3 = new Server();
        server3.setName("SuperEmbed");
        server3.setUrl("https://superembed.mov/movie/12345");
        servers.add(server3);
        
        Server server4 = new Server();
        server4.setName("YouTube");
        server4.setUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
        servers.add(server4);
        
        Server server5 = new Server();
        server5.setName("Google Drive");
        server5.setUrl("https://drive.google.com/file/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/preview");
        servers.add(server5);
        
        Server server6 = new Server();
        server6.setName("Mega");
        server6.setUrl("https://mega.nz/file/example123");
        servers.add(server6);
        
        Server server7 = new Server();
        server7.setName("MultiEmbed");
        server7.setUrl("https://multiembed.mov/directstream.php?video_id=259291&tmdb=1&s=1&e=1");
        servers.add(server7);
        
        Server server8 = new Server();
        server8.setName("Direct Stream");
        server8.setUrl("https://example.com/video.mp4");
        servers.add(server8);

        entry.setServers(servers);
        return entry;
    }

    private Entry createSampleTVSeries() {
        Entry entry = new Entry();
        entry.setTitle("Sample TV Series");
        entry.setDescription("This is a sample TV series to test the enhanced DetailsActivity functionality with seasons and episodes.");
        entry.setMainCategory("TV Series");
        entry.setSubCategory("Drama");
        entry.setDuration("45 min");
        entry.setYear(2024);
        entry.setRating(9.0f);
        entry.setPoster("https://via.placeholder.com/300x450");
        entry.setThumbnail("https://via.placeholder.com/300x450");

        // Create sample seasons
        List<Season> seasons = new ArrayList<>();
        
        // Season 1
        Season season1 = new Season();
        season1.setSeason(1);
        season1.setSeasonPoster("https://via.placeholder.com/300x450");
        
        List<Episode> episodes1 = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Episode episode = new Episode();
            episode.setEpisode(i);
            episode.setTitle("Episode " + i);
            episode.setDuration("45 min");
            episode.setDescription("This is episode " + i + " of season 1.");
            episode.setThumbnail("https://via.placeholder.com/300x200");
            
            // Create servers for each episode
            List<Server> episodeServers = new ArrayList<>();
            
            Server server1 = new Server();
            server1.setName("VidSrc.to");
            server1.setUrl("https://vidsrc.to/embed/tv/12345/1/" + i);
            episodeServers.add(server1);
            
            Server server2 = new Server();
            server2.setName("VidJoy");
            server2.setUrl("https://vidjoy.pro/embed/tv/12345-1-" + i);
            episodeServers.add(server2);
            
            Server server3 = new Server();
            server3.setName("SuperEmbed");
            server3.setUrl("https://superembed.mov/tv/12345/1/" + i);
            episodeServers.add(server3);
            
            Server server4 = new Server();
            server4.setName("YouTube");
            server4.setUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
            episodeServers.add(server4);
            
            Server server5 = new Server();
            server5.setName("Google Drive");
            server5.setUrl("https://drive.google.com/file/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/preview");
            episodeServers.add(server5);
            
            Server server6 = new Server();
            server6.setName("Mega");
            server6.setUrl("https://mega.nz/file/example123");
            episodeServers.add(server6);
            
            Server server7 = new Server();
            server7.setName("MultiEmbed");
            server7.setUrl("https://multiembed.mov/directstream.php?video_id=259291&tmdb=1&s=1&e=" + i);
            episodeServers.add(server7);

            episode.setServers(episodeServers);
            episodes1.add(episode);
        }
        season1.setEpisodes(episodes1);
        seasons.add(season1);
        
        // Season 2
        Season season2 = new Season();
        season2.setSeason(2);
        season2.setSeasonPoster("https://via.placeholder.com/300x450");
        
        List<Episode> episodes2 = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            Episode episode = new Episode();
            episode.setEpisode(i);
            episode.setTitle("Episode " + i);
            episode.setDuration("45 min");
            episode.setDescription("This is episode " + i + " of season 2.");
            episode.setThumbnail("https://via.placeholder.com/300x200");
            
            // Create servers for each episode
            List<Server> episodeServers = new ArrayList<>();
            
            Server server1 = new Server();
            server1.setName("VidSrc.to");
            server1.setUrl("https://vidsrc.to/embed/tv/12345/2/" + i);
            episodeServers.add(server1);
            
            Server server2 = new Server();
            server2.setName("VidJoy");
            server2.setUrl("https://vidjoy.pro/embed/tv/12345-2-" + i);
            episodeServers.add(server2);
            
            Server server3 = new Server();
            server3.setName("SuperEmbed");
            server3.setUrl("https://superembed.mov/tv/259291/2/" + i);
            episodeServers.add(server3);
            
            Server server4 = new Server();
            server4.setName("YouTube");
            server4.setUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
            episodeServers.add(server4);
            
            Server server5 = new Server();
            server5.setName("Google Drive");
            server5.setUrl("https://drive.google.com/file/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/preview");
            episodeServers.add(server5);
            
            Server server6 = new Server();
            server6.setName("Mega");
            server6.setUrl("https://mega.nz/file/example123");
            episodeServers.add(server6);
            
            Server server7 = new Server();
            server7.setName("MultiEmbed");
            server7.setUrl("https://multiembed.mov/directstream.php?video_id=259291&tmdb=1&s=1&e=" + i);
            episodeServers.add(server7);

            episode.setServers(episodeServers);
            episodes2.add(episode);
        }
        season2.setEpisodes(episodes2);
        seasons.add(season2);

        entry.setSeasons(seasons);
        return entry;
    }
}