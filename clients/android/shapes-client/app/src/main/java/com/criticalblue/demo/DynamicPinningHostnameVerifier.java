package com.criticalblue.demo;

import android.util.Log;

import com.criticalblue.attestationlibrary.ApproovAttestation;
import com.criticalblue.attestationlibrary.TokenInterface;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLException;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

/**
 * Created by barryo on 07/08/17.
 *
 * Inspired by “Android Security: SSL Pinning” by Matthew Dolan
 * https://medium.com/@appmattus/android-security-ssl-pinning-1db8acb6621e
 *
 * This is an example of how to implement Approov based Dynamic Pinning
 * on Android.
 *
 * This implementation of HostnameVerifier is intended to enhance the
 * HostnameVerifier your SSL implementation normally uses. The
 * HostnameVerifier passed into the constructor continues to be executed
 * when verify is called.
 *
 * The Dynamic Pinning Check is only applied if the usual HostnameVerifier
 * first passes (so this implementation can only be more secure). For dynamic
 * pinning to work you must first make a call to one of the
 * ApproovAttestation.shared().fetchApproovToken..(String pUrl) functions before
 * making a request to the same URL on a connection using this Hostname Verifier.
 *
 * Use Cases:
 *
 * -- HttpsURLConnection --
 *
 *  // Override the default Hostname verifier for new HTTPS connections
 *  URL aUrl = new URL("https://" + SERVER_HOSTNAME);
 *  HttpsURLConnection connection = (HttpsURLConnection) aUrl.openConnection();
 *  DynamicPinningHostnameVerifier verifier = new DynamicPinningHostnameVerifier(connection.getHostnameVerifier());
 *  connection.setHostnameVerifier(verifier);
 *
 *  // Create new connections as usual.
 *
 * -- OkHttp --
 *
 *  // Use the default HostnameVerifier from OkHttpClient
 *  OkHttpClient baseClient = new OkHttpClient();
 *  DynamicPinningHostnameVerifier verifier
 *      = new DynamicPinningHostnameVerifier(baseClient.hostnameVerifier());
 *
 *  // Build a new instace of the okHttpClient for this requester to use.
 *  pinnedClient = baseClient.newBuilder()
 *                  .hostnameVerifier(verifier)
 *                  .build();
 *
 *  When Executing a request, always catch SSLPeerUnverifiedException and perform
 *  a retry including a call to ApproovAttestation.shared().fetchApproovToken..(String hostname)
 */

public final class DynamicPinningHostnameVerifier implements HostnameVerifier {

    /** The HostnameVerifier you would normally be using. */
    private final HostnameVerifier delegate;

    /** Tag for log messages */
    private static final String TAG = "DYNAMIC_PINNING";

    /**
     * Construct a DynamicPinningHostnameVerifier which delegates
     * the initial verify to a user defined HostnameVerifier before
     * applying dynamic pinning on top.
     *
     * @param delegate The HostnameVerifier to apply before the Dynamic
     *                  pinning check. Typically this would be the class
     *                  used by your usual http library (i.e OkHttp) or
     *                  simply  javax.net.ssl.DefaultHostnameVerifier
     */
    public DynamicPinningHostnameVerifier(HostnameVerifier delegate) {
        this.delegate = delegate;
    }

    /**
     * Check the Approov SDK cached cert for hostname
     * against the provided Leaf Cert.
     *
     * @param hostname Name of the host we are checking the cert for.
     * @param leafCert The leaf certificate of the chain provided by the
     *                  host we are connecting to. Typically this is the 0th
     *                  element it the certificate array.
     * @return true if the the certificates match, false otherwise.
     */
    private boolean checkDynamicPinning(String hostname, Certificate leafCert) {

        //StackTraceElement[] st = Thread.currentThread().getStackTrace();

        // Check if we have the cert for the hostname in the sdk cache
        if (ApproovAttestation.shared().getCert(hostname) == null) {
            Log.w(TAG, "Approov SDK does not have a cached cert for: " + hostname);
            //for (int i=0; i < st.length; i++)
            //    Log.i(TAG, "A:" + st[i].toString());
            Log.i(TAG, "Running Token Fetch to get cert for: " + hostname);
            // Do the token fetch that we must have missed previously.
            ApproovAttestation.AttestationResult result = ApproovAttestation.shared()
                    .fetchApproovTokenAndWait(hostname).getResult();
            // If the fetch failed then we give up
            if (result == ApproovAttestation.AttestationResult.FAILURE) {
                Log.e(TAG, "Cannot fetch a cert for: " + hostname);
                return false;
            }
        }

        // The should always work now.
        byte[] certBytes = ApproovAttestation.shared().getCert(hostname);
        if (certBytes == null) {
            Log.e(TAG, "Cannot fetch a cert for: " + hostname);
            return false;
        }

        // Convert bytes into cert for comparison
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate cert = cf.generateCertificate(new ByteArrayInputStream(certBytes));

            if (cert.equals(leafCert)) {
                Log.i(TAG, "Pinning check passed for " + hostname);
                return true;
            } else {
                Log.w(TAG, "Certs do not match for: " + hostname + " - flushing SDK cert cache.");
               // for (int i=0; i < st.length; i++)
               //     Log.i(TAG, "B:" + st[i].toString());
                ApproovAttestation.shared().clearCerts();
                return false;
            }

        } catch (CertificateException e) {
            Log.w(TAG, "Failed to construct Certificate object from bytes for: " + hostname + " - flushing SDK cert cache.");
            e.printStackTrace();
            ApproovAttestation.shared().clearCerts();
            return false;
        }


    }

    @Override
    public boolean verify(String hostname, SSLSession session) {

        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        for (int i=0; i < st.length; i++)
             Log.i(TAG, "verify:" + st[i].toString());

        if (delegate.verify(hostname, session)) try {
            // Assume the leaf cert is at element 0 in the getPeerCertificates() array.
            return checkDynamicPinning(hostname, session.getPeerCertificates()[0]);
        } catch (SSLException e) {
            Log.e(TAG, "Delegate Exception");
            throw new RuntimeException(e);
        }

        Log.w(TAG, "Delegate Verify returned false");


        return false;
    }
}
