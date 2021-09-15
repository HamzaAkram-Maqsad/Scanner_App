package com.example.scannerapp

import org.opencv.android.InstallCallbackInterface
import org.opencv.android.LoaderCallbackInterface
import android.app.Activity
import androidx.annotation.NonNull





open class OpenCVCallback(val mContext:Activity) : LoaderCallbackInterface {



    override fun onManagerConnected(status: Int) {

        when (status) {
            LoaderCallbackInterface.SUCCESS -> {

            }

            LoaderCallbackInterface.MARKET_ERROR -> {

            }

            LoaderCallbackInterface.INSTALL_CANCELED -> {

            }

            LoaderCallbackInterface.INCOMPATIBLE_MANAGER_VERSION -> {

            }

            else -> {

            }
        }


    }

    override fun onPackageInstall(operation: Int, callback: InstallCallbackInterface) {
        when (operation) {
            InstallCallbackInterface.NEW_INSTALLATION -> {

            }
            InstallCallbackInterface.INSTALLATION_PROGRESS -> {

            }
        }
    }

}