package net.coding.program.project.detail;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.loopj.android.http.RequestParams;

import net.coding.program.BaseFragmentActivity;
import net.coding.program.FootUpdate;
import net.coding.program.Global;
import net.coding.program.R;
import net.coding.program.common.DialogUtil;
import net.coding.program.common.network.RefreshBaseFragment;
import net.coding.program.model.AttachmentFolderObject;
import net.coding.program.model.ProjectObject;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Created by yangzhen on 2014/10/25.
 */
@EFragment(R.layout.common_refresh_listview)
@OptionsMenu(R.menu.project_attachment_folder)
public class ProjectAttachmentFragment extends RefreshBaseFragment implements FootUpdate.LoadMore {
    private ArrayList<AttachmentFolderObject> mData = new ArrayList<AttachmentFolderObject>();
    //private String HOST_FOLDER = Global.HOST + "/api/project/%s/folders?pageSize=20";
    private String HOST_FOLDER = Global.HOST + "/api/project/%s/all_folders?pageSize=9999";
    //https://coding.net/api/project/20945/all_folders?page=1&pageSize=9999
    //private String HOST_FILECOUNT = Global.HOST + "/api/project/%s/folders/filecount";
    private String HOST_FILECOUNT = Global.HOST + "/api/project/%s/folders/all_file_count";
    //https://coding.net/api/project/20945/folders/all_file_count
    private String HOST_FOLDER_NAME = Global.HOST + "/api/project/%s/dir/%s/name/%s";
    private String HOST_FOLDER_NEW = Global.HOST + "/api/project/%s/mkdir";
    private String HOST_FOLDER_DELETE_FORMAT = Global.HOST + "/api/project/%s/rmdir/%s";
    private String HOST_FOLDER_DELETE;
    //https://coding.net/api/project/20945/rmdir/37282
    //https://coding.net/api/project/20945/mkdir?name=%E6%96%B0%E5%BB%BA%E6%96%87%E4%BB%B6%E5%A4%B9
    //https://coding.net/api/project/20945/dir/34365/name/%E6%96%B0%E5%BB%BA%E6%96%87%E4%BB%B6%E5%A4%B92

    private HashMap<String, Integer> fileCountMap = new HashMap<String, Integer>();

    //@FragmentArg
    boolean mShowAdd = true;

    @FragmentArg
    ProjectObject mProjectObject;

    @ViewById
    ListView listView;

    @ViewById
    View blankLayout;

    //@ViewById
    //SwipeRefreshLayout swipeRefreshLayout;

    public static final int RESULT_REQUEST_FILES = 1;

    @AfterViews
    protected void init() {
        super.init();
        showDialogLoading();
        initBottomPop();
        HOST_FOLDER = String.format(HOST_FOLDER, mProjectObject.id);
        HOST_FILECOUNT = String.format(HOST_FILECOUNT, mProjectObject.id);
        //mFootUpdate.init(listView, mInflater, this);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //AttachmentsActivity_.intent(getActivity()).mAttachmentFolderObject(mData.get(position)).mProjectObjectId(mProjectObject.id).start();
                AttachmentsActivity_.intent(getActivity()).mAttachmentFolderObject(mData.get(position)).mProjectObjectId(mProjectObject.id).startForResult(RESULT_REQUEST_FILES);
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                //showButtomToast("rename");
                showPop(view, position);
                //doRename(position, mData.get(position));
                return true;
            }
        });
        getNetwork(HOST_FILECOUNT, HOST_FILECOUNT);

    }

    @OnActivityResult(RESULT_REQUEST_FILES)
    public void onFileResult(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            initSetting();
            //showDialogLoading();
            setRefreshing(true);
            getNetwork(HOST_FILECOUNT, HOST_FILECOUNT);
        }
    }

    @Override
    public void onRefresh() {
        initSetting();
        //getNetwork(HOST_FOLDER, HOST_FOLDER);
        getNetwork(HOST_FILECOUNT, HOST_FILECOUNT);
    }

    @OptionsItem
    void action_add_folder() {
        doNowFolder();
    }

    //@OptionsItem
    //void action_edit_folder(){
    //    doEdit();
    //}

    @Override
    public void loadMore() {
        getNextPageNetwork(HOST_FOLDER, HOST_FOLDER);
    }

    @Override
    public void parseJson(int code, JSONObject respanse, String tag, int pos, Object data) throws JSONException {
        if (tag.equals(HOST_FOLDER)) {
            hideProgressDialog();
            setRefreshing(false);
            if (code == 0) {
                if (isLoadingFirstPage(tag)) {
                    mData.clear();
                }
                JSONArray folders = respanse.getJSONObject("data").getJSONArray("list");

                AttachmentFolderObject defaultFolder = new AttachmentFolderObject();
                defaultFolder.setCount(fileCountMap.get(defaultFolder.file_id));
                mData.add(defaultFolder);

                for (int i = 0; i < folders.length(); ++i) {
                    AttachmentFolderObject folder = new AttachmentFolderObject(folders.getJSONObject(i));
                    folder.setCount(fileCountMap.get(folder.file_id));
                    ArrayList<AttachmentFolderObject> subFolders = folder.sub_folders;
                    for (AttachmentFolderObject subFolder : subFolders) {
                        subFolder.setCount(fileCountMap.get(subFolder.file_id));
                    }
                    mData.add(folder);
                }

                adapter.notifyDataSetChanged();
            } else {
                showErrorMsg(code, respanse);
            }
        } else if (tag.equals(HOST_FILECOUNT)) {
            if (code == 0) {
                JSONArray counts = respanse.getJSONArray("data");

                for (int i = 0; i < counts.length(); ++i) {
                    JSONObject countItem = counts.optJSONObject(i);
                    fileCountMap.put(countItem.optString("folder"), countItem.optInt("count"));
                }
                loadMore();
            } else {
                showErrorMsg(code, respanse);
            }
        } else if (tag.equals(HOST_FOLDER_NAME)) {
            if (code == 0) {
                showButtomToast("重命名成功");
                AttachmentFolderObject folderObject = mData.get(pos);
                folderObject.name = (String) data;
                adapter.notifyDataSetChanged();
                //mData.clear();
                //AttachmentFolderObject folderObject = (AttachmentFolderObject)data;
                //loadMore();
            } else {
                showButtomToast("重命名失败");
            }
        } else if (tag.equals(HOST_FOLDER_NEW)) {
            if (code == 0) {
                AttachmentFolderObject folder = new AttachmentFolderObject(respanse.getJSONObject("data"));
                mData.add(1, folder);
                adapter.notifyDataSetChanged();
            } else {
                showErrorMsg(code, respanse);
            }
        } else if (tag.equals(HOST_FOLDER_DELETE)) {
            if (code == 0) {
                setRefreshing(false);
                mData.remove(selectFolder.get(0));
                selectFolder.remove(0);
                if (selectFolder.size() > 0) {
                    deleteFolders();
                } else {
                    showButtomToast("删除完成");
                    adapter.notifyDataSetChanged();
                }
            } else {
                showErrorMsg(code, respanse);
            }
        }
    }

    private boolean isEditMode = false;
    BaseAdapter adapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public Object getItem(int position) {
            return mData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.project_attachment_list_item, parent, false);
                holder = new ViewHolder();
                holder.name = (TextView) convertView.findViewById(R.id.name);
                holder.icon = (ImageView) convertView.findViewById(R.id.icon);
                holder.checkBox = (CheckBox) convertView.findViewById(R.id.checkbox);
                holder.more = (RelativeLayout) convertView.findViewById(R.id.more);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            AttachmentFolderObject data = mData.get(position);
            holder.name.setText(data.getNameCount());
            if (data.file_id.equals("0")) {
                holder.icon.setImageResource(R.drawable.ic_project_attachment_folder_2);
                holder.more.setVisibility(View.GONE);
            } else {
                holder.icon.setImageResource(R.drawable.ic_project_attachment_folder);
                holder.more.setVisibility(View.VISIBLE);
            }
            //iconfromNetwork(holder.icon, data.user.avatar);
            if (isEditMode && data.isDeleteable()) {
                holder.checkBox.setVisibility(View.VISIBLE);
                if (data.isSelected) {
                    holder.checkBox.setChecked(true);
                } else {
                    holder.checkBox.setChecked(false);
                }
            } else {
                holder.checkBox.setVisibility(View.GONE);
            }
            holder.checkBox.setOnCheckedChangeListener(onCheckedChangeListener);
            holder.checkBox.setTag(new Integer(position));

            holder.more.setOnClickListener(onMoreClickListener);
            holder.more.setTag(new Integer(position));

            if (position == mData.size() - 1) {
                loadMore();
            }

            return convertView;
        }

        private CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AttachmentFolderObject data = mData.get((Integer) buttonView.getTag());
                data.isSelected = isChecked;
            }
        };

        private View.OnClickListener onMoreClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPop(view, (Integer) view.getTag());
            }
        };
    };

    static class ViewHolder {
        ImageView icon;
        TextView name;
        CheckBox checkBox;
        RelativeLayout more;
    }

    private void doRename(final int position, final AttachmentFolderObject folderObject) {
        if (folderObject.file_id.equals("0")) {
            showButtomToast("默认文件夹无法重命名");
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //final EditText input = new EditText(getActivity());
        LayoutInflater li = LayoutInflater.from(getActivity());
        View v1 = li.inflate(R.layout.dialog_input, null);
        final EditText input = (EditText) v1.findViewById(R.id.value);
        input.setText(folderObject.name);
        builder.setTitle("重命名")
                .setView(v1).setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = input.getText().toString();
                //从网页版扒来的正则
                String namePatternStr = "[,`~!@#$%^&*:;()''\"\"><|.\\ /=]";
                Pattern namePattern = Pattern.compile(namePatternStr);
                if (newName.equals("")) {
                    showButtomToast("名字不能为空");
                } else if (namePattern.matcher(newName).find()) {
                    showButtomToast("文件夹名：" + newName + " 不能采用");
                    // if(folder.name.match(/[,`~!@#$%^&*:;()''""><|.\ /=]/g))
                } else {
                    if (!newName.equals(folderObject.name)) {
                        HOST_FOLDER_NAME = String.format(HOST_FOLDER_NAME, mProjectObject.id, folderObject.file_id, newName);
                        putNetwork(HOST_FOLDER_NAME, HOST_FOLDER_NAME, position, newName);
                    }
                }
            }
        }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        //builder.create().show();
        AlertDialog dialog = builder.create();
        dialog.show();
        ((BaseFragmentActivity) getActivity()).dialogTitleLineColor(dialog);
        input.requestFocus();
    }

    private void doNowFolder() {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //final EditText input = new EditText(getActivity());
        LayoutInflater li = LayoutInflater.from(getActivity());
        View v1 = li.inflate(R.layout.dialog_input, null);
        final EditText input = (EditText) v1.findViewById(R.id.value);
        input.setHint("请输入文件夹名称");
        builder.setTitle("新建文件夹")
                .setView(v1).setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = input.getText().toString();
                String namePatternStr = "[,`~!@#$%^&*:;()''\"\"><|.\\ /=]";
                Pattern namePattern = Pattern.compile(namePatternStr);
                if (newName.equals("")) {
                    showButtomToast("名字不能为空");
                } else if (namePattern.matcher(newName).find()) {
                    showButtomToast("文件夹名：" + newName + " 不能采用");
                    // if(folder.name.match(/[,`~!@#$%^&*:;()''""><|.\ /=]/g))
                } else {
                    HOST_FOLDER_NEW = String.format(HOST_FOLDER_NEW, mProjectObject.id);
                    RequestParams params = new RequestParams();
                    params.put("name", newName);
                    postNetwork(HOST_FOLDER_NEW, params, HOST_FOLDER_NEW);
                }
            }
        }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        //builder.create().show();
        AlertDialog dialog = builder.create();
        dialog.show();
        ((BaseFragmentActivity) getActivity()).dialogTitleLineColor(dialog);
        input.requestFocus();
    }

    ActionMode mActionMode;

    private void doEdit() {
        if (mActionMode != null) {
            return;
        }

        // Start the CAB using the ActionMode.Callback defined above
        mActionMode = getActivity().startActionMode(mActionModeCallback);
        setListEditMode(true);
        //view.setSelected(true);
    }

    private void setListEditMode(boolean isEditMode) {
        this.isEditMode = isEditMode;
        adapter.notifyDataSetChanged();
    }

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.project_attachment_folder_edit, menu);
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                //case R.id.menu_share:
                //    shareCurrentItem();
                //    mode.finish(); // Action picked, so close the CAB
                //    return true;
                case R.id.action_delete:
                    action_delete();
                    return true;
                case R.id.action_all:
                    action_all();
                    return true;
                case R.id.action_inverse:
                    action_inverse();
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            setListEditMode(false);
        }
    };

    ArrayList<AttachmentFolderObject> selectFolder;

    void action_delete() {
        selectFolder = new ArrayList<AttachmentFolderObject>();
        for (AttachmentFolderObject folderObject : mData) {
            if (folderObject.isSelected)
                selectFolder.add(folderObject);
        }
        //showButtomToast("selected count:" + selectFolder.size());
        if (selectFolder.size() == 0) {
            return;
        }
        String messageFormat = "确定要删除%s个文件夹么？";
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("删除文件夹").setMessage(String.format(messageFormat, selectFolder.size()))
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteFolders();
                    }
                }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        //builder.create().show();
        AlertDialog dialog = builder.create();
        dialog.show();
        ((BaseFragmentActivity) getActivity()).dialogTitleLineColor(dialog);
    }

    void action_delete_single(AttachmentFolderObject selectedFolderObject) {
        if (selectedFolderObject == null)
            return;

        selectFolder = new ArrayList<AttachmentFolderObject>();
        selectFolder.add(selectedFolderObject);
        String messageFormat = "确定删除文件夹\"%s\"？";
        showDialog("删除文件夹", String.format(messageFormat, selectedFolderObject.name), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteFolders();
            }
        });
    }

    void deleteFolders() {
        if (selectFolder.size() > 0) {
            setRefreshing(true);
            HOST_FOLDER_DELETE = String.format(HOST_FOLDER_DELETE_FORMAT, mProjectObject.id, selectFolder.get(0).file_id);
            deleteNetwork(HOST_FOLDER_DELETE, HOST_FOLDER_DELETE);
        }
    }

    void action_all() {
        for (AttachmentFolderObject folderObject : mData) {
            if (folderObject.isDeleteable())
                folderObject.isSelected = true;
        }
        adapter.notifyDataSetChanged();
    }

    void action_inverse() {
        for (AttachmentFolderObject folderObject : mData) {
            if (folderObject.isDeleteable())
                folderObject.isSelected = !folderObject.isSelected;
        }
        adapter.notifyDataSetChanged();
    }

    /**
     * 弹出框
     */
    private DialogUtil.BottomPopupWindow mAttachmentPopupWindow = null;
    //private AttachmentFolderObject selectedFolderObject = null;
    private int selectedPosition;

    public void initBottomPop() {
        if (mAttachmentPopupWindow == null) {
            ArrayList<DialogUtil.BottomPopupItem> popupItemArrayList = new ArrayList<DialogUtil.BottomPopupItem>();
            DialogUtil.BottomPopupItem renameItem = new DialogUtil.BottomPopupItem("重命名", R.drawable.ic_popup_attachment_rename);
            popupItemArrayList.add(renameItem);
            DialogUtil.BottomPopupItem deleteItem = new DialogUtil.BottomPopupItem("删除", R.drawable.ic_popup_attachment_delete_selector);
            popupItemArrayList.add(deleteItem);
            mAttachmentPopupWindow = DialogUtil.initBottomPopupWindow(getActivity(), "", popupItemArrayList, onPopupItemClickListener);
        }
    }

    public void showPop(View view, int position) {
        if (position == 0) {
            return;
        }
        if (mAttachmentPopupWindow == null) {
            initBottomPop();
        }
        selectedPosition = position;
        AttachmentFolderObject selectedFolderObject = mData.get(selectedPosition);
        DialogUtil.BottomPopupItem renameItem = mAttachmentPopupWindow.adapter.getItem(0);
        DialogUtil.BottomPopupItem deleteItem = mAttachmentPopupWindow.adapter.getItem(1);
        if (selectedFolderObject.file_id.equals("0")) {
            renameItem.enabled = false;
            deleteItem.enabled = false;
        } else if (selectedFolderObject.count != 0) {
            renameItem.enabled = true;
            deleteItem.enabled = false;
        } else {
            renameItem.enabled = true;
            deleteItem.enabled = true;
        }
        mAttachmentPopupWindow.adapter.notifyDataSetChanged();
        mAttachmentPopupWindow.tvTitle.setText(selectedFolderObject.name);

        mAttachmentPopupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
    }

    private AdapterView.OnItemClickListener onPopupItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            switch (position) {
                case 0:
                    //showButtomToast("rename");
                    doRename(selectedPosition, mData.get(selectedPosition));
                    break;
                case 1:
                    //showButtomToast("delete");
                    AttachmentFolderObject selectedFolderObject = mData.get(selectedPosition);
                    if (selectedFolderObject.isDeleteable()) {
                        action_delete_single(selectedFolderObject);
                    } else {
                        showButtomToast("请先清空文件夹");
                        return;
                    }
                    break;
            }
            mAttachmentPopupWindow.dismiss();
        }
    };

}
