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
package com.raytheon.viz.gfe.rsc;

import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.viz.core.contours.util.IVectorGraphicsRenderableFactory;
import com.raytheon.viz.core.contours.util.VectorGraphicsRenderable;

/**
 * GFE VectorGraphicsRenderable Factory
 * 
 * Constructs the VectorGraphicsRenderable for GFE usage of GriddedVectorDisplay
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 22, 2013     #2287  randerso     Initial creation
 * 
 * </pre>
 * 
 * @author randerso
 * @version 1.0
 */

public class GFEVectorGraphicsRenderableFactory implements
        IVectorGraphicsRenderableFactory {
    private double logFactor;

    private double maxLimit;

    /**
     * Constructor
     * 
     * @param logFactor
     *            logFactor scaling value from parm_arrowScaling preference
     * @param maxLimit
     *            max allowable value for parm
     */
    public GFEVectorGraphicsRenderableFactory(double logFactor, double maxLimit) {
        this.logFactor = logFactor;
        this.maxLimit = maxLimit;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.viz.core.contours.util.IVectorGraphicsRenderableFactory#
     * createRenderable(com.raytheon.uf.viz.core.drawables.IDescriptor,
     * com.raytheon.uf.viz.core.IGraphicsTarget, double)
     */
    @Override
    public VectorGraphicsRenderable createRenderable(IDescriptor descriptor,
            IGraphicsTarget target, double size) {
        return new GFEVectorGraphicsRenderable(descriptor, target, size,
                logFactor, maxLimit);
    }

}