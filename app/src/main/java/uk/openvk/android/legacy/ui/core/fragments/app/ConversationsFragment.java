package uk.openvk.android.legacy.ui.core.fragments.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;

import uk.openvk.android.legacy.R;
import uk.openvk.android.legacy.api.Account;
import uk.openvk.android.legacy.api.models.Conversation;
import uk.openvk.android.legacy.ui.list.adapters.ConversationsListAdapter;

/** OPENVK LEGACY LICENSE NOTIFICATION
 *
 *  This program is free software: you can redistribute it and/or modify it under the terms of
 *  the GNU Affero General Public License as published by the Free Software Foundation, either
 *  version 3 of the License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with this
 *  program. If not, see https://www.gnu.org/licenses/.
 *
 *  Source code: https://github.com/openvk/mobile-android-legacy
 **/

public class ConversationsFragment extends Fragment {
    private ArrayList<Conversation> conversations;
    private ConversationsListAdapter conversationsAdapter;
    private ListView convListView;
    private Account account;
    private View view;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_conversations, container, false);
        convListView = view.findViewById(R.id.conversations_listview);
        return view;
    }

    public void createAdapter(Context ctx, ArrayList<Conversation> conversations, Account account) {
        this.conversations = conversations;
        this.account = account;
        conversationsAdapter = new ConversationsListAdapter(ctx, this.conversations, account);
        convListView.setAdapter(conversationsAdapter);
    }

    public int getCount() {
        return convListView.getCount();
    }

    public void loadAvatars(ArrayList<Conversation> conversations_list) {
        try {
            for (int i = 0; i < conversations_list.size(); i++) {
                Conversation conversation = conversations_list.get(i);
                if (conversation.avatar_url.length() > 0) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    Bitmap bitmap = BitmapFactory.decodeFile(
                            String.format("%s/photos_cache/conversations_avatars/avatar_%s",
                                    getContext().getCacheDir(),
                            conversation.peer_id), options);
                    if (bitmap != null) {
                        conversation.avatar = bitmap;
                        conversations_list.set(i, conversation);
                    }
                }
            }
            conversations = conversations_list;
            conversationsAdapter = new ConversationsListAdapter(getContext(), conversations, account);
            convListView.setAdapter(conversationsAdapter);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
