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
package com.raytheon.uf.edex.registry.ebxml.dao;

import java.util.List;

import oasis.names.tc.ebxml.regrep.xsd.rim.v4.SlotType;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.raytheon.uf.edex.database.dao.SessionManagedDao;

/**
 * 
 * Data Access object for interacting with slot objects
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 7/11/2013    1707        bphillip    Initial implementation
 * </pre>
 * 
 * @author bphillip
 * @version 1
 */
public class SlotTypeDao extends SessionManagedDao<Integer, SlotType> {

    /** SQL query to get the ids of the orphaned slots */
    private static final String ORPHANED_SLOT_QUERY = "select key from ebxml.slot where key not in "
            + "(select child_slot_key from ebxml.action_slot "
            + "UNION "
            + "select child_slot_key from ebxml.personname_slot "
            + "UNION "
            + "select child_slot_key from ebxml.objectref_slot "
            + "UNION "
            + "select child_slot_key from ebxml.parameter_slot "
            + "UNION "
            + "select child_slot_key from ebxml.queryexpression_slot "
            + "UNION "
            + "select child_slot_key from ebxml.registryobject_slot "
            + "UNION "
            + "select child_slot_key from ebxml.query_slot "
            + "UNION "
            + "select child_slot_key from ebxml.slot_slot "
            + "UNION "
            + "select child_slot_key from ebxml.telephonenumber_slot "
            + "UNION "
            + "select child_slot_key from ebxml.deliveryinfo_slot "
            + "UNION "
            + "select child_slot_key from ebxml.emailaddress_slot "
            + "UNION "
            + "select child_slot_key from ebxml.postaladdress_slot "
            + ")";

    /** Keys parameter for the query to get the orphaned slot objects */
    private static final String GET_BY_ID_QUERY_KEYS_PARAMETER = "keys";

    /** Result batch size for the query to get the orphaned slot objects */
    private static final int GET_BY_ID_QUERY_BATCH_SIZE = 1000;

    private static final String GET_BY_ID_QUERY = "FROM SlotType slot where slot.key in (:"
            + GET_BY_ID_QUERY_KEYS_PARAMETER + ")";

    @Override
    protected Class<SlotType> getEntityClass() {
        return SlotType.class;
    }

    @SuppressWarnings("unchecked")
    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteOrphanedSlots() {
        List<Integer> orphanedSlotIds = this.getSessionFactory()
                .getCurrentSession().createSQLQuery(ORPHANED_SLOT_QUERY).list();
        if (!orphanedSlotIds.isEmpty()) {

            List<SlotType> slots = this.executeHQLQuery(GET_BY_ID_QUERY,
                    GET_BY_ID_QUERY_BATCH_SIZE, GET_BY_ID_QUERY_KEYS_PARAMETER,
                    orphanedSlotIds);
            statusHandler.info("Removing " + orphanedSlotIds.size()
                    + " orphaned slots...");
            this.deleteAll(slots);
        }
    }
}