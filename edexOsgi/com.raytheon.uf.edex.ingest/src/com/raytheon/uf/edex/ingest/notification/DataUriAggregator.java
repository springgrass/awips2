/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.edex.ingest.notification;

import java.util.ArrayList;
import java.util.List;

import com.raytheon.uf.common.dataplugin.message.DataURINotificationMessage;
import com.raytheon.uf.common.dataplugin.message.PracticeDataURINotificationMessage;

/**
 * Combines multiple messages of URIs into a single message that can be
 * triggered
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 10, 2008            njensen     Initial creation
 * Feb 15, 2013 1638       mschenke    Moved DataURINotificationMessage to uf.common.dataplugin
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public class DataUriAggregator {

    private List<String> dataUris = new ArrayList<String>();

    /**
     * Add data uris to the queue
     * 
     * @param uris
     */
    public void addDataUris(String[] uris) {
        synchronized (this) {
            for (String uri : uris) {
                if (uri != null) {
                    dataUris.add(uri);
                }
            }
        }
    }

    /**
     * Filters quartz messages to only send a message if there are URIs to send.
     * 
     * @param obj
     * @return
     */
    public boolean hasUris(Object obj) {
        synchronized (this) {
            return dataUris.size() > 0;
        }
    }

    /**
     * Send off the currently queued uris
     * 
     * @return
     */
    public DataURINotificationMessage sendQueuedUris() {
        synchronized (this) {
            String[] uris = dataUris.toArray(new String[dataUris.size()]);
            dataUris.clear();
            DataURINotificationMessage msg = new DataURINotificationMessage();
            msg.setDataURIs(uris);
            return msg;
        }
    }

    /**
     * Send off the currently queued uris
     * 
     * @return
     */
    public PracticeDataURINotificationMessage sendPracticeQueuedUris() {
        synchronized (this) {
            String[] uris = dataUris.toArray(new String[dataUris.size()]);
            dataUris.clear();
            PracticeDataURINotificationMessage msg = new PracticeDataURINotificationMessage();
            msg.setDataURIs(uris);
            return msg;
        }
    }
}