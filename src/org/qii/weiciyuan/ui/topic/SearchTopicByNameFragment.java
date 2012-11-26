package org.qii.weiciyuan.ui.topic;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;
import org.qii.weiciyuan.R;
import org.qii.weiciyuan.bean.TopicResultListBean;
import org.qii.weiciyuan.dao.topic.SearchTopicDao;
import org.qii.weiciyuan.dao.topic.TopicDao;
import org.qii.weiciyuan.support.error.WeiboException;
import org.qii.weiciyuan.support.lib.MyAsyncTask;
import org.qii.weiciyuan.support.utils.GlobalContext;
import org.qii.weiciyuan.support.utils.Utility;
import org.qii.weiciyuan.ui.basefragment.AbstractMessageTimeLineFragment;
import org.qii.weiciyuan.ui.browser.BrowserWeiboMsgActivity;
import org.qii.weiciyuan.ui.send.WriteWeiboActivity;

/**
 * User: qii
 * Date: 12-9-26
 */
public class SearchTopicByNameFragment extends AbstractMessageTimeLineFragment<TopicResultListBean> {

    private String q;
    //page 0 and page 1 data is same
    private int page = 1;

    private TopicResultListBean bean = new TopicResultListBean();

    private FollowTopicTask followTopicTask;
    private UnFollowTopicTask unFollowTopicTask;

    @Override
    public TopicResultListBean getList() {
        return bean;
    }

    public SearchTopicByNameFragment() {

    }

    public SearchTopicByNameFragment(String q) {
        this.q = q;
    }


    @Override
    public void onDetach() {
        super.onDetach();
        Utility.cancelTasks(followTopicTask, unFollowTopicTask);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("q", q);
        outState.putInt("page", page);
        outState.putSerializable("bean", bean);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            q = savedInstanceState.getString("q");
            page = savedInstanceState.getInt("page");
            bean = (TopicResultListBean) savedInstanceState.getSerializable("bean");
        } else {
            getPullToRefreshListView().startRefreshNow();

        }
    }

    @Override
    protected void newMsgOnPostExecute(TopicResultListBean newValue) {
        if (newValue != null && getActivity() != null && newValue.getSize() > 0) {
            getList().addNewData(newValue);
            getAdapter().notifyDataSetChanged();
            getListView().setSelectionAfterHeaderView();
            buildActionBatSubtitle();
        }
    }

    @Override
    protected void oldMsgOnPostExecute(TopicResultListBean newValue) {
        if (newValue != null && newValue.getSize() > 0) {
            getList().addOldData(newValue);
            page++;
            buildActionBatSubtitle();
        }
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.actionbar_menu_searchtopicbynamefragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_write:
                Intent intent = new Intent(getActivity(), WriteWeiboActivity.class);
                intent.putExtra("token", GlobalContext.getInstance().getSpecialToken());
                intent.putExtra("account", GlobalContext.getInstance().getAccountBean());
                intent.putExtra("content", "#" + q + "#");
                startActivity(intent);
                break;

            case R.id.menu_refresh:
                pullToRefreshListView.startRefreshNow();
                break;
            case R.id.menu_follow_topic:
                if (Utility.isTaskStopped(followTopicTask)) {
                    followTopicTask = new FollowTopicTask();
                    followTopicTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
                }
                break;
            case R.id.menu_unfollow_topic:
                if (Utility.isTaskStopped(unFollowTopicTask)) {
                    unFollowTopicTask = new UnFollowTopicTask();
                    unFollowTopicTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void listViewItemClick(AdapterView parent, View view, int position, long id) {
        Intent intent = new Intent(getActivity(), BrowserWeiboMsgActivity.class);
        intent.putExtra("msg", bean.getItemList().get(position));
        intent.putExtra("token", GlobalContext.getInstance().getSpecialToken());
        startActivity(intent);
    }

    @Override
    protected TopicResultListBean getDoInBackgroundNewData() throws WeiboException {

        page = 1;
        SearchTopicDao dao = new SearchTopicDao(GlobalContext.getInstance().getSpecialToken(), q);
        dao.setPage(String.valueOf(page));
        TopicResultListBean result = dao.getGSONMsgList();

        return result;
    }

    @Override
    protected TopicResultListBean getDoInBackgroundMiddleData(String beginId, String endId) throws WeiboException {
        return null;
    }


    private void buildActionBatSubtitle() {
        int newSize = bean.getTotal_number();
        String number = bean.getSize() + "/" + newSize;
        getActivity().getActionBar().setSubtitle(number);
    }

    @Override
    protected TopicResultListBean getDoInBackgroundOldData() throws WeiboException {
        SearchTopicDao dao = new SearchTopicDao(GlobalContext.getInstance().getSpecialToken(), q);
        dao.setPage(String.valueOf(page + 1));
        TopicResultListBean result = dao.getGSONMsgList();
        return result;
    }

    private class FollowTopicTask extends MyAsyncTask<Void, Boolean, Boolean> {
        WeiboException e;

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                return new TopicDao(GlobalContext.getInstance().getSpecialToken()).follow(q);
            } catch (WeiboException e) {
                this.e = e;
                cancel(true);
            }
            return false;
        }

        @Override
        protected void onCancelled(Boolean aBoolean) {
            super.onCancelled(aBoolean);
            if (getActivity() != null && e != null) {
                Toast.makeText(getActivity(), e.getError(), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (getActivity() == null)
                return;
            if (aBoolean)
                Toast.makeText(getActivity(), getString(R.string.follow_topic_successfully), Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(getActivity(), getString(R.string.follow_topic_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private class UnFollowTopicTask extends MyAsyncTask<Void, Boolean, Boolean> {
        WeiboException e;

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                return new TopicDao(GlobalContext.getInstance().getSpecialToken()).destroy(q);
            } catch (WeiboException e) {
                this.e = e;
                cancel(true);
            }
            return false;
        }

        @Override
        protected void onCancelled(Boolean aBoolean) {
            super.onCancelled(aBoolean);
            if (getActivity() != null && e != null) {
                Toast.makeText(getActivity(), e.getError(), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (getActivity() == null)
                return;
            if (aBoolean)
                Toast.makeText(getActivity(), getString(R.string.unfollow_topic_successfully), Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(getActivity(), getString(R.string.unfollow_topic_failed), Toast.LENGTH_SHORT).show();
        }

    }
}
