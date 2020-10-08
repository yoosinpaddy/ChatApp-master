package com.Trichain.chatapp.holders;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.recyclerview.widget.RecyclerView;

import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.Trichain.chatapp.R;
import com.Trichain.chatapp.activities.FullScreenActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.content.Context.DOWNLOAD_SERVICE;

/**
 * This is a part of ChatApp Project (https://github.com/h01d/ChatApp)
 * Licensed under Apache License 2.0
 *
 * @author Raf (https://github.com/h01d)
 * @version 1.1
 * @since 27/02/2018
 */

public class MessageHolder extends RecyclerView.ViewHolder {
    private final String TAG = "CA/MessageHolder";

    private View view;
    private Context context;

    // Will handle User, Chat and Chat Typing data

    private DatabaseReference userDatabase, chatSeenDatabase, chatTypingDatabase;
    private ValueEventListener userListener, chatSeenListener, chatTypingListener;
    RelativeLayout messageLayoutLeft;
    RelativeLayout messageLayoutRight;
    TextView messageTextRight;
    TextView messageTimeRight;
    CircleImageView messageImageRight;
    ImageView messageTextPictureRight;
    TextView messageLoadingRight,messageLoadingLeft ;

    public MessageHolder(View view, Context context) {
        super(view);

        this.view = view;
        this.context = context;
        // If this an upcoming message

        messageLayoutLeft = view.findViewById(R.id.message_relative_left);

        messageLayoutRight = view.findViewById(R.id.message_relative_right);
        messageTextRight = view.findViewById(R.id.message_text_right);
        messageTimeRight = view.findViewById(R.id.message_time_right);
        messageImageRight = view.findViewById(R.id.message_image_right);
        messageTextPictureRight = view.findViewById(R.id.message_imagetext_right);
        messageLoadingRight = view.findViewById(R.id.message_loading_right);
        messageLoadingLeft = view.findViewById(R.id.message_loading_left);
    }

    public void hideBottom() {
        final RelativeLayout messageBottom = view.findViewById(R.id.message_relative_bottom);

        messageBottom.setVisibility(View.GONE);
    }

    public void setLastMessage(final String currentUserId, final String from, final String to) {
        // If the message is the last message in the list

        final TextView messageSeen = view.findViewById(R.id.message_seen);
        final TextView messageTyping = view.findViewById(R.id.message_typing);

        final RelativeLayout messageBottom = view.findViewById(R.id.message_relative_bottom);

        messageBottom.setVisibility(View.VISIBLE);

        String otherUserId = from;

        if (from.equals(currentUserId)) {
            otherUserId = to;

            if (chatSeenDatabase != null && chatSeenListener != null) {
                chatSeenDatabase.removeEventListener(chatSeenListener);
            }

            // Initialize/Update seen message on the bottom of the message

            chatSeenDatabase = FirebaseDatabase.getInstance().getReference().child("Chat").child(to).child(currentUserId);
            chatSeenListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    try {
                        if (from.equals(currentUserId) && dataSnapshot.hasChild("seen")) {
                            messageSeen.setVisibility(View.VISIBLE);

                            long seen = (long) dataSnapshot.child("seen").getValue();

                            if (seen == 0) {
                                messageSeen.setText("Sent");
                            } else {
                                messageSeen.setText("Seen at " + new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(seen));
                            }
                        } else {
                            messageSeen.setVisibility(View.INVISIBLE);
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "chatSeenListerner exception: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.d(TAG, "chatSeenListerner failed: " + databaseError.getMessage());
                }
            };
            chatSeenDatabase.addValueEventListener(chatSeenListener);
        } else {
            messageSeen.setVisibility(View.INVISIBLE);
        }

        if (chatTypingDatabase != null && chatTypingListener != null) {
            chatTypingDatabase.removeEventListener(chatTypingListener);
        }

        // Initialize/Update typing status on the bottom

        chatTypingDatabase = FirebaseDatabase.getInstance().getReference().child("Chat").child(otherUserId).child(currentUserId);
        chatTypingListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    if (dataSnapshot.hasChild("typing")) {
                        int typing = Integer.parseInt(dataSnapshot.child("typing").getValue().toString());

                        messageTyping.setVisibility(View.VISIBLE);

                        if (typing == 1) {
                            messageTyping.setText("Typing...");
                        } else if (typing == 2) {
                            messageTyping.setText("Deleting...");
                        } else if (typing == 3) {
                            messageTyping.setText("Thinking...");
                        } else {
                            messageTyping.setVisibility(View.INVISIBLE);
                        }
                    } else {
                        messageTyping.setVisibility(View.INVISIBLE);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "chatTypingListener exception: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "chatTypingListener failed: " + databaseError.getMessage());
            }
        };
        chatTypingDatabase.addValueEventListener(chatTypingListener);
    }

    public void setRightMessage(String userid, final String message, long time, String type, final MessageHolder holder) {

        messageLayoutLeft.setVisibility(View.GONE);

        messageLayoutRight.setVisibility(View.VISIBLE);

        if (type.equals("text")) {
            messageTextPictureRight.setVisibility(View.GONE);
            messageLoadingRight.setVisibility(View.GONE);

            messageTextRight.setVisibility(View.VISIBLE);
            messageTextRight.setText(message);
        } else if (type.equals("image")) {
            messageTextRight.setVisibility(View.GONE);

            messageTextPictureRight.setVisibility(View.VISIBLE);
            messageLoadingRight.setVisibility(View.VISIBLE);
            messageLoadingRight.setText("Loading picture...");

            Picasso.with(context)
                    .load(message)
                    .fit()
                    .networkPolicy(NetworkPolicy.OFFLINE)
                    .into(messageTextPictureRight, new Callback() {
                        @Override
                        public void onSuccess() {
                            messageLoadingRight.setVisibility(View.GONE);
                        }

                        @Override
                        public void onError() {
                            Picasso.with(context)
                                    .load(message)
                                    .fit()
                                    .into(messageTextPictureRight, new Callback() {
                                        @Override
                                        public void onSuccess() {
                                            messageLoadingRight.setVisibility(View.GONE);
                                        }

                                        @Override
                                        public void onError() {
                                            messageLoadingRight.setText("Error: could not load picture.");
                                        }
                                    });
                        }
                    });

            messageTextPictureRight.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(context, FullScreenActivity.class);
                    intent.putExtra("imageUrl", message);
                    context.startActivity(intent);
                }
            });
        } else {
            messageTextRight.setVisibility(View.GONE);

            messageTextPictureRight.setVisibility(View.VISIBLE);
            messageLoadingRight.setVisibility(View.VISIBLE);
            messageLoadingRight.setText("Loading file...");

            Picasso.with(context)
                    .load(R.drawable.myfikle_24)
                    .fit()
                    .networkPolicy(NetworkPolicy.OFFLINE)
                    .into(messageTextPictureRight, new Callback() {
                        @Override
                        public void onSuccess() {
                            messageLoadingRight.setVisibility(View.GONE);
                        }

                        @Override
                        public void onError() {
                            Picasso.with(context)
                                    .load(R.drawable.myfikle_24)
                                    .fit()
                                    .into(messageTextPictureRight, new Callback() {
                                        @Override
                                        public void onSuccess() {
                                            messageLoadingRight.setVisibility(View.GONE);
                                        }

                                        @Override
                                        public void onError() {
                                            messageLoadingRight.setText("Click to open");
                                        }
                                    });
                        }
                    });

            messageTextPictureRight.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String url = message;
                    Uri uri = Uri.parse(String.valueOf(message));
                    Toast.makeText(context, "Downloading...", Toast.LENGTH_LONG).show();
                    downloadMedia(url, context, holder);
//                    Intent intent = new Intent(Intent.ACTION_VIEW);
//                    // Check what kind of file you are trying to open, by comparing the url with extensions.
//                    // When the if condition is matched, plugin sets the correct intent (mime) type,
//                    // so Android knew what application to use to open the file
//                    if (url.toString().contains(".doc") || url.toString().contains(".docx")) {
//                        // Word document
//                        intent.setDataAndType(uri, "application/msword");
//                    } else if (url.toString().contains(".pdf")) {
//                        // PDF file
//                        intent.setDataAndType(uri, "application/pdf");
//                    } else if (url.toString().contains(".ppt") || url.toString().contains(".pptx")) {
//                        // Powerpoint file
//                        intent.setDataAndType(uri, "application/vnd.ms-powerpoint");
//                    } else if (url.toString().contains(".xls") || url.toString().contains(".xlsx")) {
//                        // Excel file
//                        intent.setDataAndType(uri, "application/vnd.ms-excel");
//                    } else if (url.toString().contains(".zip") || url.toString().contains(".rar")) {
//                        // WAV audio file
//                        intent.setDataAndType(uri, "application/x-wav");
//                    } else if (url.toString().contains(".rtf")) {
//                        // RTF file
//                        intent.setDataAndType(uri, "application/rtf");
//                    } else if (url.toString().contains(".wav") || url.toString().contains(".mp3")) {
//                        // WAV audio file
//                        intent.setDataAndType(uri, "audio/x-wav");
//                    } else if (url.toString().contains(".gif")) {
//                        // GIF file
//                        intent.setDataAndType(uri, "image/gif");
//                    } else if (url.toString().contains(".jpg") || url.toString().contains(".jpeg") || url.toString().contains(".png")) {
//                        // JPG file
//                        intent.setDataAndType(uri, "image/jpeg");
//                    } else if (url.toString().contains(".txt")) {
//                        // Text file
//                        intent.setDataAndType(uri, "text/plain");
//                    } else if (url.toString().contains(".3gp") || url.toString().contains(".mpg") || url.toString().contains(".mpeg") || url.toString().contains(".mpe") || url.toString().contains(".mp4") || url.toString().contains(".avi")) {
//                        // Video files
//                        intent.setDataAndType(uri, "video/*");
//                    } else {
//                        //if you want you can also define the intent type for any other file
//
//                        //additionally use else clause below, to manage other unknown extensions
//                        //in this case, Android will show all applications installed on the device
//                        //so you can choose which application to use
//                        intent.setDataAndType(uri, "*/*");
//                    }
//
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    context.startActivity(intent);
                }
            });
        }

        messageTimeRight.setText(DateUtils.isToday(time) ? new SimpleDateFormat("HH:mm", Locale.getDefault()).format(time) : new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(time));

        if (userDatabase != null && userListener != null) {
            userDatabase.removeEventListener(userListener);
        }

        // Initialize/Update user image

        userDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(userid);
        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                try {
                    final String image = dataSnapshot.child("image").getValue().toString();

                    if (!image.equals("default")) {
                        Picasso.with(context)
                                .load(image)
                                .resize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, context.getResources().getDisplayMetrics()), (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, context.getResources().getDisplayMetrics()))
                                .centerCrop()
                                .networkPolicy(NetworkPolicy.OFFLINE)
                                .placeholder(R.drawable.user)
                                .into(messageImageRight, new Callback() {
                                    @Override
                                    public void onSuccess() {

                                    }

                                    @Override
                                    public void onError() {
                                        Picasso.with(context)
                                                .load(image)
                                                .resize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, context.getResources().getDisplayMetrics()), (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, context.getResources().getDisplayMetrics()))
                                                .centerCrop()
                                                .placeholder(R.drawable.user)
                                                .error(R.drawable.user)
                                                .into(messageImageRight);
                                    }
                                });
                    } else {
                        messageImageRight.setImageResource(R.drawable.user);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "userDatabase exception: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "userDatabase failed: " + databaseError.getMessage());
            }
        };
        userDatabase.addValueEventListener(userListener);
    }

    private void downloadMedia(String url, Context context, MessageHolder holder) {
        if (messageLoadingRight != null) {
            messageLoadingRight.setText("Downloading...");
        }
        if (messageLayoutLeft != null) {
            messageLoadingLeft.setText("Downloading...");
        }

        DownloadManager dm = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(
                Uri.parse(url));
//        String root10=context.getExternalFilesDir(null).getAbsolutePath();
        String root = Environment.getExternalStorageDirectory().toString();
//        Log.e(TAG, "saveVideo1: "+root10 );
        Log.e(TAG, "download: " + url);
        File myDir = new File(root + "/P2PChat");
        String imageName = "P2P-" + System.currentTimeMillis() + ".";
        if (imageName.lastIndexOf(".") != -1 && imageName.lastIndexOf(".") != 0) {
            imageName += url.substring(url.lastIndexOf(".") + 1);
        } else {
            imageName += "mp4";
        }
        File file = new File(myDir, imageName);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String otherRoot, otherNewRoot = "";
            otherRoot = context.getExternalFilesDir(null).getAbsolutePath();//android 10 and above
            otherNewRoot = otherRoot.split("emulated/0")[0] + "emulated/0/P2PChat/";
            //request.setDestinationInExternalFilesDir(context, file.getAbsolutePath(), imageName);//android 10 and above
            request.setDestinationInExternalFilesDir(context, "/P2PChat/", imageName);//android 10 and above
            Log.e(TAG, "saveVideo: FILE:: " + file.getAbsolutePath());
            String finalImageName = imageName;
            String finalOtherNewRoot = otherNewRoot;
            context.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.e(TAG, "onReceive: ");

                    if (messageLoadingRight != null) {
                        messageLoadingRight.setText("Downloaded");
                    }
                    if (messageLayoutLeft != null) {
                        messageLoadingLeft.setText("Downloaded");
                    }
//                    File source = new File(context.getExternalFilesDir(null).getAbsolutePath(), finalImageName);
                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();
//                    moveFile(source, finalOtherNewRoot +finalImageName);
                }
            }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        } else {
            request.setDestinationUri(Uri.parse("file://" + file.getAbsolutePath())).setNotificationVisibility(1);// android 9 and below
            context.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.e(TAG, "onReceive: ");

                    if (messageLoadingRight != null) {
                        messageLoadingRight.setText("Downloaded");
                    }
                    if (messageLayoutLeft != null) {
                        messageLoadingLeft.setText("Downloaded");
                    }
//                    File source = new File(context.getExternalFilesDir(null).getAbsolutePath(), finalImageName);
                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();
//                    moveFile(source, finalOtherNewRoot +finalImageName);
                }
            }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
        Log.e(TAG, "saveVideo: " + file.getAbsolutePath());
        dm.enqueue(request);

        Toast.makeText(context, "Downloading...", Toast.LENGTH_SHORT).show();

    }

    public void setLeftMessage(String userid, final String message, long time, String type, final MessageHolder holder) {
        // If this is a sent message

        final RelativeLayout messageLayoutRight = view.findViewById(R.id.message_relative_right);

        final RelativeLayout messageLayoutLeft = view.findViewById(R.id.message_relative_left);
        final TextView messageTextLeft = view.findViewById(R.id.message_text_left);
        final TextView messageTimeLeft = view.findViewById(R.id.message_time_left);
        final CircleImageView messageImageLeft = view.findViewById(R.id.message_image_left);
        final ImageView messageTextPictureLeft = view.findViewById(R.id.message_imagetext_left);
        messageLoadingLeft = view.findViewById(R.id.message_loading_left);

        messageLayoutRight.setVisibility(View.GONE);

        messageLayoutLeft.setVisibility(View.VISIBLE);

        if (type.equals("text")) {
            messageTextPictureLeft.setVisibility(View.GONE);
            messageLoadingLeft.setVisibility(View.GONE);

            messageTextLeft.setVisibility(View.VISIBLE);
            messageTextLeft.setText(message);
        } else if (type.equals("image")) {
            messageTextLeft.setVisibility(View.GONE);

            messageTextPictureLeft.setVisibility(View.VISIBLE);
            messageLoadingLeft.setVisibility(View.VISIBLE);
            messageLoadingLeft.setText("Loading picture...");

            Picasso.with(context)
                    .load(message)
                    .fit()
                    .networkPolicy(NetworkPolicy.OFFLINE)
                    .into(messageTextPictureLeft, new Callback() {
                        @Override
                        public void onSuccess() {
                            messageLoadingLeft.setVisibility(View.GONE);
                        }

                        @Override
                        public void onError() {
                            Picasso.with(context)
                                    .load(message)
                                    .fit()
                                    .into(messageTextPictureLeft, new Callback() {
                                        @Override
                                        public void onSuccess() {
                                            messageLoadingLeft.setVisibility(View.GONE);
                                        }

                                        @Override
                                        public void onError() {
                                            messageLoadingLeft.setText("Error: could not load picture.");
                                        }
                                    });
                        }
                    });

            messageTextPictureLeft.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(context, FullScreenActivity.class);
                    intent.putExtra("imageUrl", message);
                    context.startActivity(intent);
                }
            });
        } else {
            messageTextLeft.setVisibility(View.GONE);

            messageTextPictureLeft.setVisibility(View.VISIBLE);
            messageLoadingLeft.setVisibility(View.VISIBLE);
            messageLoadingLeft.setText("Loading File...");

            Picasso.with(context)
                    .load(R.drawable.myfikle_24)
                    .fit()
                    .networkPolicy(NetworkPolicy.OFFLINE)
                    .into(messageTextPictureLeft, new Callback() {
                        @Override
                        public void onSuccess() {
                            messageLoadingLeft.setVisibility(View.GONE);
                        }

                        @Override
                        public void onError() {
                            Picasso.with(context)
                                    .load(R.drawable.myfikle_24)
                                    .fit()
                                    .into(messageTextPictureLeft, new Callback() {
                                        @Override
                                        public void onSuccess() {
                                            messageLoadingLeft.setVisibility(View.GONE);
                                        }

                                        @Override
                                        public void onError() {
                                            messageLoadingLeft.setText("Click to open");
                                        }
                                    });
                        }
                    });

            messageTextPictureLeft.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String url = message;
                    Uri uri = Uri.parse(String.valueOf(message));
                    downloadMedia(url, context, holder);
//
//                    Intent intent = new Intent(Intent.ACTION_VIEW);
//                    // Check what kind of file you are trying to open, by comparing the url with extensions.
//                    // When the if condition is matched, plugin sets the correct intent (mime) type,
//                    // so Android knew what application to use to open the file
//                    if (url.toString().contains(".doc") || url.toString().contains(".docx")) {
//                        // Word document
//                        intent.setDataAndType(uri, "application/msword");
//                    } else if (url.toString().contains(".pdf")) {
//                        // PDF file
//                        intent.setDataAndType(uri, "application/pdf");
//                    } else if (url.toString().contains(".ppt") || url.toString().contains(".pptx")) {
//                        // Powerpoint file
//                        intent.setDataAndType(uri, "application/vnd.ms-powerpoint");
//                    } else if (url.toString().contains(".xls") || url.toString().contains(".xlsx")) {
//                        // Excel file
//                        intent.setDataAndType(uri, "application/vnd.ms-excel");
//                    } else if (url.toString().contains(".zip") || url.toString().contains(".rar")) {
//                        // WAV audio file
//                        intent.setDataAndType(uri, "application/x-wav");
//                    } else if (url.toString().contains(".rtf")) {
//                        // RTF file
//                        intent.setDataAndType(uri, "application/rtf");
//                    } else if (url.toString().contains(".wav") || url.toString().contains(".mp3")) {
//                        // WAV audio file
//                        intent.setDataAndType(uri, "audio/x-wav");
//                    } else if (url.toString().contains(".gif")) {
//                        // GIF file
//                        intent.setDataAndType(uri, "image/gif");
//                    } else if (url.toString().contains(".jpg") || url.toString().contains(".jpeg") || url.toString().contains(".png")) {
//                        // JPG file
//                        intent.setDataAndType(uri, "image/jpeg");
//                    } else if (url.toString().contains(".txt")) {
//                        // Text file
//                        intent.setDataAndType(uri, "text/plain");
//                    } else if (url.toString().contains(".3gp") || url.toString().contains(".mpg") || url.toString().contains(".mpeg") || url.toString().contains(".mpe") || url.toString().contains(".mp4") || url.toString().contains(".avi")) {
//                        // Video files
//                        intent.setDataAndType(uri, "video/*");
//                    } else {
//                        //if you want you can also define the intent type for any other file
//
//                        //additionally use else clause below, to manage other unknown extensions
//                        //in this case, Android will show all applications installed on the device
//                        //so you can choose which application to use
//                        intent.setDataAndType(uri, "*/*");
//                    }
//
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    context.startActivity(intent);
                }
            });
        }
        messageTimeLeft.setText(DateUtils.isToday(time) ? new SimpleDateFormat("HH:mm", Locale.getDefault()).format(time) : new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(time));

        if (userDatabase != null && userListener != null) {
            userDatabase.removeEventListener(userListener);
        }

        // Initilize/Update user image

        userDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(userid);
        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                try {
                    final String image = dataSnapshot.child("image").getValue().toString();

                    if (!image.equals("default")) {
                        Picasso.with(context)
                                .load(image)
                                .resize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, context.getResources().getDisplayMetrics()), (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, context.getResources().getDisplayMetrics()))
                                .centerCrop()
                                .networkPolicy(NetworkPolicy.OFFLINE)
                                .placeholder(R.drawable.user)
                                .into(messageImageLeft, new Callback() {
                                    @Override
                                    public void onSuccess() {

                                    }

                                    @Override
                                    public void onError() {
                                        Picasso.with(context)
                                                .load(image)
                                                .resize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, context.getResources().getDisplayMetrics()), (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, context.getResources().getDisplayMetrics()))
                                                .centerCrop()
                                                .placeholder(R.drawable.user)
                                                .error(R.drawable.user)
                                                .into(messageImageLeft);
                                    }
                                });
                    } else {
                        messageImageLeft.setImageResource(R.drawable.user);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "userDatabase exception: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "userDatabase failed: " + databaseError.getMessage());
            }
        };
        userDatabase.addValueEventListener(userListener);
    }
}
