package com.dozuki.ifixit.dozuki.ui;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.widget.SearchView;
import com.dozuki.ifixit.MainApplication;
import com.dozuki.ifixit.R;
import com.dozuki.ifixit.dozuki.model.Site;
import com.dozuki.ifixit.topic_view.ui.TopicsActivity;
import com.dozuki.ifixit.util.APIEndpoint;
import com.dozuki.ifixit.util.APIError;
import com.dozuki.ifixit.util.APIReceiver;
import com.dozuki.ifixit.util.APIService;

import java.util.ArrayList;

public class SiteListActivity extends SherlockFragmentActivity
 implements SearchView.OnQueryTextListener {
   private static final String SITE_LIST = "SITE_LIST";

   private Button mSiteListButton;
   private ArrayList<Site> mSiteList;
   private ListView mSiteListView;
   private SearchView mSearchView;

   private APIReceiver mApiReceiver = new APIReceiver() {
      @SuppressWarnings("unchecked")
      public void onSuccess(Object result, Intent intent) {
         mSiteList = (ArrayList<Site>)result;
         setSiteList(mSiteList);
      }

      public void onFailure(APIError error, Intent intent) {
         APIService.getErrorDialog(SiteListActivity.this, error,
          APIService.getSitesIntent(SiteListActivity.this)).show();
      }
   };

   @SuppressWarnings("unchecked")
   @Override
   public void onCreate(Bundle savedInstanceState) {
      getSupportActionBar().hide();

      super.onCreate(savedInstanceState);

      setContentView(R.layout.site_list);

      if (savedInstanceState != null) {
         mSiteList = (ArrayList<Site>)savedInstanceState.getSerializable(
          SITE_LIST);
      }

      if (mSiteList == null) {
         getSiteList();
      }

      mSiteListButton = (Button)findViewById(R.id.list_dialog_btn);
      Typeface btnType = Typeface.createFromAsset(getAssets(), "fonts/ProximaNovaRegular.otf");
      mSiteListButton.setTypeface(btnType);

      mSiteListButton.setOnClickListener(new OnClickListener() {
         public void onClick(View view) {
            /**
             * TODO: It should probably always open up the list dialog even if
             * we don't have the site list yet. Then once we get it we can
             * update the list.
             */
            if (mSiteList != null) {
               showSiteListDialog(mSiteList);
            }
         }
      });

      handleIntent(getIntent());
   }

   @Override
   protected void onNewIntent(Intent intent) {
      setIntent(intent);
      handleIntent(intent);
   }

   private void handleIntent(Intent intent) {
      if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
         String query = intent.getStringExtra(SearchManager.QUERY);
         search(query);
      }
   }

   private void search(String query) {
      String lowerQuery = query.toLowerCase();
      ArrayList<Site> matchedSites = new ArrayList<Site>();

      for (Site site : mSiteList) {
         if (site.search(lowerQuery)) {
            matchedSites.add(site);
         }
      }

      setSiteList(matchedSites);
   }

   private void cancelSearch() {
      setSiteList(mSiteList);
   }

   @Override
   public void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);

      outState.putSerializable(SITE_LIST, mSiteList);
   }

   @Override
   public void onResume() {
      super.onResume();

      IntentFilter filter = new IntentFilter();
      filter.addAction(APIEndpoint.SITES.mAction);
      registerReceiver(mApiReceiver, filter);
   }

   @Override
   public void onPause() {
      super.onPause();

      try {
         unregisterReceiver(mApiReceiver);
      } catch (IllegalArgumentException e) {
         // Do nothing. This happens in the unlikely event that
         // unregisterReceiver has been called already.
      }
   }

   /**
    * Sets the ListView and SearchView so this Activity can proxy searches through.
    */
   protected void setSiteListViews(ListView siteListView, SearchView searchView) {
      mSiteListView = siteListView;
      mSearchView = searchView;

      SearchManager searchManager = (SearchManager)getSystemService(
       Context.SEARCH_SERVICE);

      searchView.setSearchableInfo(searchManager.getSearchableInfo(
       getComponentName()));
      searchView.setIconifiedByDefault(false);
      searchView.setOnQueryTextListener(this);

      setSiteList(mSiteList);
   }

   public boolean onQueryTextChange(String newText) {
      if (newText.length() == 0) {
         cancelSearch();
      } else {
         // Perform search on every key press.
         search(newText);
      }

      return false;
   }

   public boolean onQueryTextSubmit(String query) {
      return false;
   }

   public boolean onClose() {
      return false;
   }

   protected boolean isAlwaysExpanded() {
      return false;
   }

   @Override
   public boolean onKeyUp(int keyCode, KeyEvent event) {
      if (keyCode == KeyEvent.KEYCODE_SEARCH) {
         /**
          * Phones with a hardware search button open up the SearchDialog by
          * default. This overrides that by setting focus on the SearchView.
          * Unfortunately it does not open the soft keyboard as of now.
          */
         mSearchView.requestFocus();
         return true;
      } else {
         return super.onKeyUp(keyCode, event);
      }
   }

   private void setSiteList(ArrayList<Site> sites) {
      if (mSiteListView == null || mSiteList == null) {
         return;
      }

      final SiteListAdapter siteListAdapter = new SiteListAdapter(sites);
      mSiteListView.setAdapter(siteListAdapter);

      mSiteListView.setOnItemClickListener(new OnItemClickListener() {
         @Override
         public void onItemClick(AdapterView<?> arg0, View view, int position,
          long id) {
            MainApplication application = ((MainApplication)getApplication());
            Intent intent = new Intent(SiteListActivity.this,
             TopicsActivity.class);

            application.setSite(siteListAdapter.getSiteList().get(position));
            startActivity(intent);
         }
      });
   }

   private void getSiteList() {
      startService(APIService.getSitesIntent(this));
   }

   private void showSiteListDialog(ArrayList<Site> sites) {
       FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
       Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
       if (prev != null) {
          ft.remove(prev);
       }
       ft.addToBackStack(null);

       // Create and show the dialog.
       DialogFragment siteList = SiteListDialogFragment.newInstance();
       siteList.show(ft, "dialog");
   }
}
