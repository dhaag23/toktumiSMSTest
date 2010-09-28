package com.toktumi.toktumiSMSTest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class SMSTest extends Activity {
    /** Tag string for our debug logs */
    private static final String TAG = "SMSTest";

    public static final String SMS_RECIPIENT_EXTRA = "com.toktumi.toktumiSMSTest.SMS_RECIPIENT";
    public static final String ACTION_SMS_SENT = "com.toktumi.toktumiSMSTest.SMS_SENT_ACTION";

	private static final int PICK_RECIPIENTS = 23;
	private static final int PICK_CONTENT = 24;
    
	private String bulkContent;
	private ArrayList<String> recipients;
	private int recipientIndex;

	private boolean cancelSend;
	private boolean sendingMessages;
	private TextView smsBulkStatusText; 
	private SmsManager smsManager;
	private Button sendBulkButton;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        smsManager = SmsManager.getDefault();
        
        setContentView(R.layout.main);

        smsBulkStatusText = (TextView)findViewById(R.id.sms_bulk_status);
        
        if (getIntent().hasExtra(SMS_RECIPIENT_EXTRA)) {
            ((TextView) findViewById(R.id.sms_recipient)).setText(getIntent().getExtras().getString(SMS_RECIPIENT_EXTRA));
            ((TextView) findViewById(R.id.sms_content)).requestFocus();
        }

        // Enable or disable the broadcast receiver depending on the checked
        // state of the checkbox.
        CheckBox enableCheckBox = (CheckBox) findViewById(R.id.sms_enable_receiver);

        final PackageManager pm = this.getPackageManager();
        final ComponentName componentName = new ComponentName("com.toktumi.toktumiSMSTest", "com.toktumi.toktumiSMSTest.SMSTest");

        enableCheckBox.setChecked(pm.getComponentEnabledSetting(componentName) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED);

        enableCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG, (isChecked ? "Enabling" : "Disabling") + " SMS receiver");

                pm.setComponentEnabledSetting(componentName,
                        isChecked ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
            }
        });

        final EditText recipientTextEdit = (EditText) this.findViewById(R.id.sms_recipient);
        final EditText contentTextEdit = (EditText) this.findViewById(R.id.sms_content);
//        final TextView statusView = (TextView) this.findViewById(R.id.sms_status);

        // Watch for send button clicks and send text messages.
        Button sendButton = (Button) findViewById(R.id.sms_send_message);
        sendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (TextUtils.isEmpty(recipientTextEdit.getText())) {
                    Toast.makeText(SMSTest.this, "Please enter a message recipient.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(contentTextEdit.getText())) {
                    Toast.makeText(SMSTest.this, "Please enter a message body.", Toast.LENGTH_SHORT).show();
                    return;
                }

//                recipientTextEdit.setEnabled(false);
//                contentTextEdit.setEnabled(false);

                List<String> messages = smsManager.divideMessage(contentTextEdit.getText().toString());

                String recipient = recipientTextEdit.getText().toString();
                for (String message : messages) {
                	smsManager.sendTextMessage(recipient, null, message, PendingIntent.getBroadcast(SMSTest.this, 0, new Intent(ACTION_SMS_SENT), 0), null);
                }
            }
        });

//        // Register broadcast receivers for SMS sent and delivered intents
//        registerReceiver(new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                String message = null;
//                boolean error = true;
//                switch (getResultCode()) {
//                case Activity.RESULT_OK:
//                    message = "Message sent!";
//                    error = false;
//                    break;
//                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
//                    message = "Error.";
//                    break;
//                case SmsManager.RESULT_ERROR_NO_SERVICE:
//                    message = "Error: No service.";
//                    break;
//                case SmsManager.RESULT_ERROR_NULL_PDU:
//                    message = "Error: Null PDU.";
//                    break;
//                case SmsManager.RESULT_ERROR_RADIO_OFF:
//                    message = "Error: Radio off.";
//                    break;
//                }
//
//                recipientTextEdit.setEnabled(true);
//                contentTextEdit.setEnabled(true);
//                contentTextEdit.setText("");
//
//                statusView.setText(message);
//                statusView.setTextColor(error ? Color.RED : Color.GREEN);
//            }
//        }, new IntentFilter(ACTION_SMS_SENT));
        
        // Register broadcast receivers for SMS sent and delivered intents
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                case Activity.RESULT_OK:
                	Log.i(TAG, "OK: " + recipients.get(recipientIndex));
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                case SmsManager.RESULT_ERROR_NULL_PDU:
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                	Log.i(TAG, "Error: " + recipients.get(recipientIndex));
                    break;
                default:
                	Log.i(TAG, "Unknonw: " + recipients.get(recipientIndex));
                    break;
                }
            }
        }, new IntentFilter(ACTION_SMS_SENT));
        
        
        

        
        
        Button selectRecipientsFileButton = (Button) findViewById(R.id.sms_recipients_file_selection);
        selectRecipientsFileButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent("org.openintents.action.PICK_FILE");
				intent.putExtra("org.openintents.extra.TITLE", "Pick recipients file");
				startActivityForResult(intent, PICK_RECIPIENTS);
			}
		});
        
        Button selectMessageFileButton = (Button) findViewById(R.id.sms_message_file_selection);
        selectMessageFileButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent("org.openintents.action.PICK_FILE");
				intent.putExtra("org.openintents.extra.TITLE", "Pick message file");
				startActivityForResult(intent, PICK_CONTENT);
			}
		});
        
        
        // Watch for send button clicks and send text messages.
        sendBulkButton = (Button)findViewById(R.id.sms_send_bulk_message);
        sendBulkButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	if (sendingMessages) {
                    sendBulkButton.setText("Cancelling");
                    cancelSend = true;
                    return;
            	}
            	
                if (recipients.size() <= 0) {
                    Toast.makeText(SMSTest.this, "Please enter a message recipient file.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(bulkContent)) {
                    Toast.makeText(SMSTest.this, "Please enter a message body file.", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                final List<String> messages = smsManager.divideMessage(bulkContent);
                sendMessagesAsync(messages, recipients);
                
                sendBulkButton.setText("Cancel");
            }
        });
        
        
    }


    
    private void sendMessagesAsync(final List<String> messages, final ArrayList<String> recipients) {
    	sendingMessages = true;
        cancelSend = false;
        recipientIndex = 0;
    	
        doSendMessageAsync(recipients.get(recipientIndex), messages);
    }

    

	private void doSendMessageAsync(String recipient, final List<String> messages) {
		smsBulkStatusText.setText(String.format("Sending #%d to: %s", recipientIndex, recipient));
		
		SMSSendAsyncTask smsSendAsyncTask = new SMSSendAsyncTask(messages);
		smsSendAsyncTask.execute(recipient);
	}    
    
    
    private class SMSSendAsyncTask extends AsyncTask<String, Void, Boolean> {
    	
    	private List<String> messages;

    	public SMSSendAsyncTask(final List<String> messages) {
    		this.messages = messages;
    	}

    	@Override
        protected Boolean doInBackground(String... recipient) {
            for (String message : messages) {
            	smsManager.sendTextMessage(recipient[0], null, message, PendingIntent.getBroadcast(SMSTest.this, 0, new Intent(ACTION_SMS_SENT), 0), null);
                try {
					Thread.sleep(2100);
				} catch (InterruptedException e) {
					return false;
				}
            }
            
            return !cancelSend;
        }

        @Override
        protected void onPostExecute(Boolean continueSend) {
        	recipientIndex++;
        	if (continueSend && (recipientIndex < recipients.size()))
        		doSendMessageAsync(recipients.get(recipientIndex), messages);
        	else {
        		sendingMessages = false;
        		smsBulkStatusText.setText("Done");
                SMSTest.this.sendBulkButton.setText(R.string.sms_send_bulk_message);
        	}
        }
    };

    
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PICK_RECIPIENTS:
            	Uri recipientsFileUri = data.getData();
            	String recipientFile = readFileAsString(recipientsFileUri.getPath());
            	String[] recipientArray = recipientFile.split(",");
            	recipients = new ArrayList<String>(recipientArray.length);
            	
            	for (String recipient : recipientArray) {
            		recipient = recipient.trim();
            		if (!TextUtils.isEmpty(recipient))
            			recipients.add(recipient);
            	}
                return;
            case PICK_CONTENT:
            	Uri contentsFileUri = data.getData();
            	bulkContent = readFileAsString(contentsFileUri.getPath());
                return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private static String readFileAsString(String filePath) {
    	try {
	        StringBuffer fileData = new StringBuffer(1000);
	        BufferedReader reader = new BufferedReader(new FileReader(filePath));
	        char[] buf = new char[1024];
	        int numRead=0;
	        while((numRead=reader.read(buf)) != -1){
	            String readData = String.valueOf(buf, 0, numRead);
	            fileData.append(readData);
	            buf = new char[1024];
	        }
	        reader.close();
	        return fileData.toString();
    	}
    	catch (IOException ex) {
    		return "";
    	}
    }

}