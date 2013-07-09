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
package com.raytheon.uf.edex.datadelivery.bandwidth;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.Lists;
import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.BandwidthBucket;
import com.raytheon.uf.edex.datadelivery.bandwidth.dao.IBandwidthBucketDao;
import com.raytheon.uf.edex.datadelivery.bandwidth.retrieval.RetrievalPlan;
import com.raytheon.uf.edex.datadelivery.bandwidth.util.BandwidthUtil;

/**
 * Extracted from {@link RetrievalPlan}. This will be replaced with a database
 * version.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 18, 2013 2106       djohnson     Extracted from {@link RetrievalPlan}.
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */

public class InMemoryBandwidthBucketDao implements IBandwidthBucketDao {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(InMemoryBandwidthBucketDao.class);

    private static final AtomicLong idSequence = new AtomicLong(1);

    private final Map<Network, NavigableMap<Long, BandwidthBucket>> allBuckets = new EnumMap<Network, NavigableMap<Long, BandwidthBucket>>(
            Network.class);
    {
        for (Network network : Network.values()) {
            allBuckets.put(network, new TreeMap<Long, BandwidthBucket>());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void create(BandwidthBucket bandwidthBucket) {
        NavigableMap<Long, BandwidthBucket> buckets = allBuckets
                .get(bandwidthBucket.getNetwork());
        if (bandwidthBucket.getId() == BandwidthUtil.DEFAULT_IDENTIFIER) {
            bandwidthBucket.setIdentifier(idSequence.incrementAndGet());
        }
        buckets.put(bandwidthBucket.getBucketStartTime(), bandwidthBucket);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(BandwidthBucket bandwidthBucket) {
        NavigableMap<Long, BandwidthBucket> buckets = allBuckets
                .get(bandwidthBucket.getNetwork());
        final long bucketStartTime = bandwidthBucket.getBucketStartTime();
        if (buckets.containsKey(bucketStartTime)) {
            buckets.put(bucketStartTime, bandwidthBucket);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteEmptyBucketsUpToTime(long timeToDeleteUpTo,
            Network network) {
        NavigableMap<Long, BandwidthBucket> buckets = allBuckets.get(network);
        NavigableMap<Long, BandwidthBucket> x = buckets.headMap(
                timeToDeleteUpTo, true);
        Iterator<Long> itr = x.keySet().iterator();
        while (itr.hasNext()) {
            Long key = itr.next();
            BandwidthBucket b = x.get(key);
            // If the bucket is empty, remove it from the Map,
            // which should result in removal from the parent Map,
            if (b.isEmpty()) {
                statusHandler.info("resize() - Removing bucket ["
                        + b.getBucketStartTime() + "]");
                itr.remove();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<BandwidthBucket> getAll(Network network) {
        final NavigableMap<Long, BandwidthBucket> buckets = allBuckets
                .get(network);
        return copyAndWrapInUnmodifiableList(buckets.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BandwidthBucket getLastBucket(Network network) {
        final NavigableMap<Long, BandwidthBucket> buckets = allBuckets
                .get(network);
        return buckets.lastEntry().getValue().copy();
    }

    @Override
    public List<BandwidthBucket> getWhereStartTimeIsLessThanOrEqualTo(
            long time, Network network) {
        final NavigableMap<Long, BandwidthBucket> buckets = allBuckets
                .get(network);
        SortedMap<Long, BandwidthBucket> available = buckets
                .headMap(time, true);
        return copyAndWrapInUnmodifiableList(available.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BandwidthBucket getFirstBucket(Network network) {
        final NavigableMap<Long, BandwidthBucket> buckets = allBuckets
                .get(network);
        return buckets.firstEntry().getValue().copy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BandwidthBucket getByStartTime(long bucketId, Network network) {
        BandwidthBucket bucket = getActualBucketByStartTime(bucketId, network);
        return (bucket == null) ? bucket : bucket.copy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SortedSet<BandwidthBucket> getBucketsInWindow(Long startMillis,
            Long endMillis, Network network) {
        // Get the bucket for the startTime and endTime.
        Long startKey = ceilingKey(startMillis, network);
        Long endKey = floorBucket(endMillis, network);

        // Handle the case where an invalid range was somehow specified
        // (shouldn't happen, so just throw an exception with as much
        // information as we have)
        if (startKey == null || endKey == null) {
            throw new IllegalArgumentException(
                    String.format(
                            "Invalid start and end times requested for getBucketsInWindow(): start time [%s], end time [%s], bucket start key [%s], bucket end key [%s].",
                            startMillis, endMillis, startKey, endKey));
        }

        final NavigableMap<Long, BandwidthBucket> buckets = allBuckets
                .get(network);
        NavigableMap<Long, BandwidthBucket> window = buckets.subMap(startKey,
                true, endKey, true);
        return new TreeSet<BandwidthBucket>(
                copyAndWrapInUnmodifiableList(window.values()));
    }

    /**
     * Returns the greatest key less than or equal to the given key, or null if
     * there is no such key.
     * 
     * @param key
     * @return the floored key, or null
     */
    private Long floorBucket(long key, Network network) {
        final NavigableMap<Long, BandwidthBucket> buckets = allBuckets
                .get(network);
        return buckets.floorKey(key);
    }

    /**
     * Returns the least key greater than or equal to the given key, or null if
     * there is no such key.
     * 
     * @param key
     * @return the ceiling-ed key, or null
     */
    private Long ceilingKey(long key, Network network) {
        final NavigableMap<Long, BandwidthBucket> buckets = allBuckets
                .get(network);
        return buckets.ceilingKey(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyState(IBandwidthBucketDao bucketsDao) {
        this.allBuckets.clear();
        for (Network network : Network.values()) {
            final TreeMap<Long, BandwidthBucket> buckets = new TreeMap<Long, BandwidthBucket>();
            this.allBuckets.put(network, buckets);
            for (BandwidthBucket bucket : bucketsDao.getAll(network)) {
                buckets.put(bucket.getBucketStartTime(), bucket.copy());
            }
        }
    }

    /**
     * Copies each {@link BandwidthBucket} and places them in an unmodifiable
     * list.
     * 
     * @param buckets
     *            the buckets
     * @return the list
     */
    private List<BandwidthBucket> copyAndWrapInUnmodifiableList(
            Collection<BandwidthBucket> buckets) {
        List<BandwidthBucket> retVal = Lists.newArrayList();
        for (BandwidthBucket bucket : buckets) {
            retVal.add(bucket.copy());
        }
        return Collections.unmodifiableList(retVal);
    }

    /**
     * Get the real {@link BandwidthBucket} instance by its start time.
     * 
     * @return {@link BandwidthBucket}
     */
    private BandwidthBucket getActualBucketByStartTime(long bucketId,
            Network network) {
        final NavigableMap<Long, BandwidthBucket> buckets = allBuckets
                .get(network);
        BandwidthBucket bucket = buckets.get(bucketId);
        return bucket;
    }
}