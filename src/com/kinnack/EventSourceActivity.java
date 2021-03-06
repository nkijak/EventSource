package com.kinnack;

import java.io.IOException;

import com.kinnack.EventSource.EventSourceListener;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class EventSourceActivity extends Activity implements EventSourceListener {
    private EventSource eventSource;
    private TextView textArea;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        textArea = (TextView)findViewById(R.id.textArea);
        try
        {
            eventSource = new EventSource("192.168.1.145", this);
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    @Override
    public void onStart()
    {
        textArea.setText(textArea.getText()+"\nStarted");
        
    }
    
    @Override
    public void onMessage(final String message)
    {
        textArea.post(new Runnable()
        {
            
            @Override
            public void run()
            {
                textArea.setText(textArea.getText()+"\n>"+message);
            }
        });
        
    }
    
    @Override
    public void onClose()
    {
        // TODO Auto-generated method stub
        
    }
    
    
    
    @Override
    protected void onPause()
    {
        super.onPause();
        if (eventSource != null) eventSource.kill();
    }
}