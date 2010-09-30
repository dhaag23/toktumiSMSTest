package com.toktumi.toktumiSMSTest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
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
import android.os.Environment;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SMSTest extends Activity {
    /** Tag string for our debug logs */
    private static final String TAG = "SMSTest";

    public static final String SMS_RECIPIENT_EXTRA = "com.toktumi.toktumiSMSTest.SMS_RECIPIENT";
    public static final String ACTION_SMS_SENT = "com.toktumi.toktumiSMSTest.SMS_SENT_ACTION";

	private static final int PICK_RECIPIENTS = 23;
	private static final int PICK_CONTENT = 24;
    
	private String bulkContent;
	private LinkedList<Recipient> recipientList;
	private int sendMessageIndex;

	private boolean cancelSend;
	private boolean sendingMessages;
	private TextView smsBulkStatusText; 
	private SmsManager smsManager;
	private Button sendBulkButton;
    private File okLog;
    private File errorLog;
    private File errorDetailsLog;
    private List<String> smsMessages;
    private int sleepTime = 2100;
    private long lastFailureTime = 0;

	private static final String SMS_INTENT_EXTRA = "com.toktumi.SMSTest.Phone"; 
	
   
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
        enableCheckBox.setChecked(true);
        enableCheckBox.setEnabled(false);

        final PackageManager pm = this.getPackageManager();
        final ComponentName componentName = new ComponentName("com.toktumi.toktumiSMSTest", "com.toktumi.toktumiSMSTest.SMSTest");
        pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

//        enableCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                Log.d(TAG, (isChecked ? "Enabling" : "Disabling") + " SMS receiver");
//
//                pm.setComponentEnabledSetting(componentName,
//                        isChecked ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
//                        PackageManager.DONT_KILL_APP);
//            }
//        });

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
                	Intent intent = new Intent(ACTION_SMS_SENT);
                	smsManager.sendTextMessage(recipient, null, message, PendingIntent.getBroadcast(SMSTest.this, 0, intent, 0), null);
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
            	synchronized (SMSTest.this) {
                	handleBroadcast(intent, getResultCode());
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
            	
                if (recipientList.size() <= 0) {
                    Toast.makeText(SMSTest.this, "Please enter a message recipient file.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(bulkContent)) {
                    Toast.makeText(SMSTest.this, "Please enter a message body file.", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                smsMessages = smsManager.divideMessage(bulkContent);
                sendMessagesAsync(smsMessages, recipientList);
                
                sendBulkButton.setText("Cancel");
            }
        });
    }

	private void handleBroadcast(Intent intent, int resultCode) {
		String recipientNumber = "unknown";
		Recipient recipient = null;
		if (intent != null) {
			recipient = (Recipient)intent.getSerializableExtra(SMS_INTENT_EXTRA);
			recipientNumber = recipient.recipientNumber; 
		}

		if (resultCode == Activity.RESULT_OK) {
        	Log.i(TAG, "OK: " + recipientNumber);
        	appendRecipient(okLog, recipientNumber);
        	
        	if (((System.currentTimeMillis() - lastFailureTime) > 30000) && (sleepTime > 2100)) {
            	sleepTime -= 10;
            	Log.i(TAG, "Sleep time reduced to = " + sleepTime);
        	}
        	return;
		}
		
		String errorType;
		switch (resultCode) {
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
            	errorType = "RESULT_ERROR_GENERIC_FAILURE";
            	break;
            case SmsManager.RESULT_ERROR_NO_SERVICE:
            	errorType = "RESULT_ERROR_NO_SERVICE";
            	break;
            case SmsManager.RESULT_ERROR_NULL_PDU:
            	errorType = "RESULT_ERROR_NULL_PDU";
            	break;
            case SmsManager.RESULT_ERROR_RADIO_OFF:
            	errorType = "RESULT_ERROR_RADIO_OFF";
            	break;
            default:
            	errorType = "Unknown";
            	break;
        }
        
		String logtext = recipient + " error = " + errorType;
        appendRecipient(errorDetailsLog, logtext);

    	lastFailureTime = System.currentTimeMillis();
        if ((recipient != null) && (recipient.trys < 3)) {
        	recipient.trys++;
    		Log.w(TAG, "Retry " + ((Integer)recipient.trys).toString() + ": " + recipientNumber);
           	recipientList.addFirst(recipient);

        	if (sleepTime < 10000) {
        		sleepTime += 100;
            	Log.i(TAG, "Sleep time increased to = " + sleepTime);
        	}
        }
        else {
       		Log.e(TAG, "Error: " + recipientNumber);
        	appendRecipient(errorLog, recipientNumber);
        }
	}
    
    private void sendMessagesAsync(final List<String> smsMessages, final LinkedList<Recipient> recipients) {
    	sendingMessages = true;
        cancelSend = false;
        sendMessageIndex = 0;
    	
        try {
			setupLogFiles();
		} catch (Exception e) {
			Toast.makeText(this, "Yikes! Can't open logs.", Toast.LENGTH_LONG);
		}

        doSendMessageAsync(recipients.removeFirst());
    }

    private void sendNextMessage() {
    	synchronized (SMSTest.this) {
        	if (!cancelSend && (recipientList.size() > 0))
        		doSendMessageAsync(recipientList.removeFirst());
        	else {
        		sendingMessages = false;
        		smsBulkStatusText.setText("Done");
                SMSTest.this.sendBulkButton.setText(R.string.sms_send_bulk_message);
        	}
		}
    }

	private void doSendMessageAsync(Recipient recipient) {
		if (recipient.trys == 0) {
	    	sendMessageIndex++;
			recipient.sendMessageIndex = sendMessageIndex;  
			smsBulkStatusText.setText(String.format("Sending #%d to: %s", recipient.sendMessageIndex, recipient.recipientNumber));
		}
		else
			smsBulkStatusText.setText(String.format("Retry (%d) #%d to: %s", recipient.trys, recipient.sendMessageIndex, recipient.recipientNumber));
		
		SMSSendAsyncTask smsSendAsyncTask = new SMSSendAsyncTask(smsMessages);
		smsSendAsyncTask.execute(recipient);
	}    
    
    
	private static int pendingIntentUniqueId = 0;
    private class SMSSendAsyncTask extends AsyncTask<Recipient, Void, Boolean> {
    	private List<String> messages;

    	public SMSSendAsyncTask(final List<String> messages) {
    		this.messages = messages;
    	}

    	private int getNextUniqueId() {
    		synchronized (SMSTest.this) {
    			pendingIntentUniqueId++;
    			return pendingIntentUniqueId;
    		}
    	}

    	@Override
        protected Boolean doInBackground(Recipient... recipient) {
            try {
    			Thread.sleep(sleepTime);
    		} catch (InterruptedException e) {
    		}

    		Recipient theRecipient = recipient[0];
        	Intent intent = new Intent(ACTION_SMS_SENT);
        	intent.putExtra(SMS_INTENT_EXTRA, theRecipient);

        	String message = messages.get(0);
        	PendingIntent pendingIntent = PendingIntent.getBroadcast(SMSTest.this, getNextUniqueId(), intent, PendingIntent.FLAG_ONE_SHOT);
    		smsManager.sendTextMessage(theRecipient.recipientNumber, null, message, pendingIntent, null);
            
            return !cancelSend;
    	}

    	@Override
        protected void onPostExecute(Boolean continueSend) {
			sendNextMessage();
        }
    
    };
    
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PICK_RECIPIENTS:
            	Uri recipientsFileUri = data.getData();
            	String recipientFile = readFileAsString(recipientsFileUri.getPath());
            	String[] recipientNumberArray = recipientFile.split(",");
            	recipientList = new LinkedList<Recipient>();
            	
            	for (String recipientNumber : recipientNumberArray) {
            		recipientNumber = recipientNumber.trim();
            		if (!TextUtils.isEmpty(recipientNumber))
            			recipientList.addLast(new Recipient(recipientNumber));
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


    private void appendRecipient(File logfile, String recipient) {
    	synchronized (SMSTest.this) {
	    	BufferedWriter writer = null;
			try {
				writer = new BufferedWriter(new FileWriter(logfile, true));
		    	writer.append(recipient + ", ");
			} catch (IOException e) {
	        	Log.e(TAG, "Error: " + e.toString());
			}
			finally {
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException e) {
					}
				}
			}
		}
    }

    private void setupLogFiles() throws IOException {
	    File sdRoot = Environment.getExternalStorageDirectory();

	    String logPath = sdRoot.getPath() + "/smstestlogs"; 
	    File logDir = new File(logPath);
	    logDir.mkdir();
	    
	    okLog = getAvailableFile(logPath, "sms_ok");
	    errorLog = getAvailableFile(logPath, "sms_error");
	    errorDetailsLog = getAvailableFile(logPath, "sms_error_details");
    }

    private File getAvailableFile(String dir, String base) {
    	File availableFile;
    	int index = 1;
    	do {
    		availableFile = new File(String.format("%s/%s-%d.txt", dir, base, index));
    		index++;
    	} while(availableFile.exists());
    		
   		return availableFile;
    }

}