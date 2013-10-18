package com.zhan_dui.adapters;

import java.util.HashMap;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.widget.CursorAdapter;
import android.text.Html;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.zhan_dui.data.Memo;
import com.zhan_dui.data.MemoProvider;
import com.zhan_dui.evermemo.MemoActivity;
import com.zhan_dui.evermemo.R;
import com.zhan_dui.utils.DateHelper;
import com.zhan_dui.utils.MarginAnimation;

public class MemosAdapter extends CursorAdapter implements OnClickListener,
		OnTouchListener {

	private LayoutInflater mLayoutInflater;
	private int mOutItemId;
	private int mBottomMargin;
	private GestureDetectorCompat mGestureDetectorCompat;

	@SuppressWarnings("unused")
	private View mTempAnimationView;
	@SuppressWarnings("unused")
	private View mPreviousTouchHover;
	private View mPreviousTouchBottom;
	private View mCurrentTouchHover;
	private View mCurrentTouchBottom;
	private int mCurrentTouchPosition;
	private boolean mCheckMode;
	private HashMap<Integer, Memo> mCheckedItems;
	private Typeface mRobotoThin;
	private final DeleteRecoverPanelLisener mDeleteRecoverPanelLisener;
	private final ItemGuestureDetector itemGuestureDetector = new ItemGuestureDetector();
	private ItemLongPressedLisener mItemLongPressedLisener;

	public void setOpenerItem(int id) {
		if (id < 0 && id > getCount()) {
			mOutItemId = 0;
		}
		mOutItemId = id;
	}

	public MemosAdapter(Context context, Cursor c, int flags,
			DeleteRecoverPanelLisener l) {
		super(context, c, flags);
		mBottomMargin = -mContext.getResources().getDimensionPixelSize(
				R.dimen.bottom_margin_left);
		mLayoutInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mGestureDetectorCompat = new GestureDetectorCompat(mContext,
				itemGuestureDetector);
		mRobotoThin = Typeface.createFromAsset(context.getAssets(),
				"fonts/Roboto-Thin.ttf");
		mDeleteRecoverPanelLisener = l;
		mContext.getContentResolver().registerContentObserver(
				MemoProvider.MEMO_URI, false,
				new UpdateObserver(mUpdateHandler));
	}

	public MemosAdapter(Context context, Cursor c, int flags,
			DeleteRecoverPanelLisener l,
			ItemLongPressedLisener itemLongPressedLisener) {
		this(context, c, flags, l);
		mItemLongPressedLisener = itemLongPressedLisener;
	}

	@SuppressLint("HandlerLeak")
	private Handler mUpdateHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			notifyDataSetChanged();
		};
	};

	class UpdateObserver extends ContentObserver {
		private Handler mHandler;

		public UpdateObserver(Handler handler) {
			super(handler);
			mHandler = handler;
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			mHandler.sendEmptyMessage(0);
		}

	}

	public View getView(int position, View convertView, ViewGroup parent) {
		if (!mDataValid) {
			throw new IllegalStateException(
					"this should only be called when the cursor is valid");
		}
		if (!mCursor.moveToPosition(position)) {
			throw new IllegalStateException("couldn't move cursor to position "
					+ position);
		}
		View v;
		if (convertView == null) {
			v = newView(mContext, mCursor, parent);
		} else {
			v = convertView;
			boolean isFirst = (Boolean) v.getTag(R.string.memo_first);
			if (isFirst && position != 0) {
				v = newView(mContext, mCursor, parent);
			}
			if (!isFirst && position == 0) {
				v = newView(mContext, mCursor, parent);
			}
		}

		if (position != 0) {
			LinearLayout.LayoutParams layoutParams = (LayoutParams) v
					.findViewById(R.id.bottom).getLayoutParams();
			if (position == mOutItemId) {
				layoutParams.setMargins(0, 0, 0, 0);
			} else {
				if (layoutParams.leftMargin == 0)
					layoutParams.setMargins(-mBottomMargin, 0, 0, 0);
			}
			v.findViewById(R.id.bottom).setLayoutParams(layoutParams);
			v.findViewById(R.id.hover).setTag(R.string.memo_position, position);
		}
		bindView(v, mContext, mCursor);

		return v;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		int _id = cursor.getInt(cursor.getColumnIndex("_id"));
		if (cursor != null && view != null && _id != 0) {
			Memo memo = new Memo(cursor);
			TextView content = (TextView) view.findViewById(R.id.content);
			TextView date = (TextView) view.findViewById(R.id.date);
			content.setText(Html.fromHtml(memo.getContent()));
			date.setText(DateHelper.getGridDate(mContext, memo.getCreatedTime()));
			View bottomView = view.findViewById(R.id.bottom);
			View hoverView = view.findViewById(R.id.hover);
			View uploadView = view.findViewById(R.id.uploading);
			bottomView.setTag(R.string.memo_data, memo);
			bottomView.setTag(R.string.memo_id, _id);
			hoverView.setTag(R.string.memo_data, memo);
			hoverView.setTag(R.string.memo_id, _id);
			if (memo.isSyncingUp()) {
				uploadView.setVisibility(View.VISIBLE);
			} else {
				uploadView.setVisibility(View.INVISIBLE);
			}
			if (mCheckMode) {
				if (isChecked(memo.getId())) {
					hoverView.setBackgroundColor(mContext.getResources()
							.getColor(R.color.item_selected_color));
				} else {
					hoverView.setBackgroundDrawable(mContext.getResources()
							.getDrawable(R.drawable.hover_background));
				}
			} else {
				hoverView.setBackgroundDrawable(mContext.getResources()
						.getDrawable(R.drawable.hover_background));
			}
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		if (cursor.getInt(cursor.getColumnIndex("_id")) == 0) {
			View firstView = mLayoutInflater.inflate(R.layout.memo_add, parent,
					false);
			firstView.setTag(R.string.memo_first, true);
			TextView textView = (TextView) firstView.findViewById(R.id.plus);
			textView.setTypeface(mRobotoThin);
			firstView.setOnClickListener(this);
			return firstView;
		} else {
			View commonView = mLayoutInflater.inflate(R.layout.memo_item,
					parent, false);
			commonView.setTag(R.string.memo_first, false);
			View bottom = commonView.findViewById(R.id.bottom);
			View hover = commonView.findViewById(R.id.hover);
			hover.setTag(bottom);
			bottom.setOnClickListener(this);
			hover.setOnClickListener(this);
			hover.setOnTouchListener(this);
			return commonView;
		}
	}

	@Override
	public void onClick(View v) {
		long span = System.currentTimeMillis() - mLastChangeStatus;
		if (span < 500) {
			return;
		}

		if (v.getTag(R.string.memo_first) != null
				&& (Boolean) v.getTag(R.string.memo_first) == true) {
			mContext.startActivity(new Intent(mContext, MemoActivity.class));
		} else {
			switch (v.getId()) {
			case R.id.bottom:
				if (mOutItemId != 0) {
					Memo memo = (Memo) v.getTag(R.string.memo_data);
					mDeleteRecoverPanelLisener.wakeRecoveryPanel(memo);
					setOpenerItem(0);
					mContext.getContentResolver().delete(
							ContentUris.withAppendedId(MemoProvider.MEMO_URI,
									memo.getId()), null, null);
				}
				break;
			case R.id.hover:
				Memo memo = (Memo) v.getTag(R.string.memo_data);
				if (mCheckMode) {
					toggleCheckedId(memo.getId(), memo);
				} else {
					Intent intent = new Intent(mContext, MemoActivity.class);
					intent.putExtra("memo", memo);
					mContext.startActivity(intent);
				}
				break;
			default:
				break;
			}
		}

	}

	private long mLastChangeStatus = System.currentTimeMillis();
	private Memo mCurrentLongPressMemo;

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		mCurrentTouchHover = v;
		mCurrentTouchBottom = (View) v.getTag();
		mCurrentTouchPosition = (Integer) v.getTag(R.string.memo_position);
		mCurrentLongPressMemo = (Memo) v.getTag(R.string.memo_data);
		if (event.getAction() == MotionEvent.ACTION_UP) {
			mLastChangeStatus = System.currentTimeMillis();
		}
		return mGestureDetectorCompat.onTouchEvent(event);
	}

	public interface ItemLongPressedLisener {
		public void onMemoItemLongClick(View view, int posotion, Memo memo);
	}

	class ItemGuestureDetector extends SimpleOnGestureListener {

		@Override
		public void onLongPress(MotionEvent e) {
			if (mItemLongPressedLisener != null) {
				mItemLongPressedLisener.onMemoItemLongClick(mCurrentTouchHover,
						mCurrentTouchPosition, mCurrentLongPressMemo);
			}
			mLastChangeStatus = System.currentTimeMillis();
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			mLastChangeStatus = 0;
			return super.onSingleTapUp(e);
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {

			if (distanceY > 20)
				return true;

			float moveDistance = e1.getX() - e2.getX();

			if (moveDistance > 0 && moveDistance < 20) {
				return true;
			}

			if (moveDistance > -25 && moveDistance < 0) {
				return true;
			}
			if (moveDistance > 0) {
				// 右向左滑
				mCurrentTouchBottom.startAnimation(new MarginAnimation(
						(LinearLayout) mCurrentTouchBottom, -mBottomMargin, 0,
						0, 0));
				setOpenerItem(0);
			} else if (moveDistance < -0) {
				// 左向右滑
				setOpenerItem(mCurrentTouchPosition);
				mCurrentTouchBottom.startAnimation(new MarginAnimation(
						(LinearLayout) mCurrentTouchBottom, 0, 0, 0, 0));
			}

			if (mPreviousTouchBottom != null
					&& mPreviousTouchBottom != mCurrentTouchBottom) {
				LayoutParams layoutParams = (LayoutParams) mPreviousTouchBottom
						.getLayoutParams();
				layoutParams.leftMargin = -mBottomMargin;
				mPreviousTouchBottom.setLayoutParams(layoutParams);
			}
			mPreviousTouchBottom = mCurrentTouchBottom;
			mTempAnimationView = mCurrentTouchBottom;
			mPreviousTouchHover = mCurrentTouchHover;
			mLastChangeStatus = System.currentTimeMillis();
			return true;
		}
	}

	public interface DeleteRecoverPanelLisener {
		public void wakeRecoveryPanel(Memo memo);
	}

	public void setCheckMode(boolean check) {
		mCheckMode = check;
		if (mCheckMode == false) {
			mCheckedItems = null;
		}
		notifyDataSetChanged();
	}

	@SuppressLint("UseSparseArrays")
	public void toggleCheckedId(int _id, Memo memo) {
		if (mCheckedItems == null) {
			mCheckedItems = new HashMap<Integer, Memo>();
		}
		if (mCheckedItems.containsKey(_id) == false) {
			mCheckedItems.put(_id, memo);
		} else {
			mCheckedItems.remove(_id);
		}
		notifyDataSetChanged();
	}

	public boolean isChecked(int _id) {
		if (mCheckedItems == null || mCheckedItems.containsKey(_id) == false) {
			return false;
		} else {
			return true;
		}
	}
}
