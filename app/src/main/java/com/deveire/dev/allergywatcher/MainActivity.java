package com.deveire.dev.allergywatcher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity
{

    private Button driverButton;
    private Button managerButton;
    private Button orderButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        driverButton = (Button) findViewById(R.id.driverButton);
        managerButton = (Button) findViewById(R.id.managerButton);
        orderButton = (Button) findViewById(R.id.orderButton);

        driverButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startActivity(new Intent(getApplicationContext(), DriverActivity.class));
            }
        });

        managerButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startActivity(new Intent(getApplicationContext(), SetupPatronActivity.class));
            }
        });

        orderButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                startActivity(new Intent(getApplicationContext(), OrderFoodActivity.class));
            }
        });

    }


}








/*


 */