package com.deveire.dev.allergywatcher;

/**
 * Created by owenryan on 18/08/2017.
 */

public interface OrderDismissObserver
{
    void callBack(int inNumberOfDrinksOrdered, OrderView callingOrder);
}

