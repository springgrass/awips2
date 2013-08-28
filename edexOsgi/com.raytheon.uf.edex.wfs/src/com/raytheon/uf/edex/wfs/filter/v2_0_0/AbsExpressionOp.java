/*
 * The following software products were developed by Raytheon:
 *
 * ADE (AWIPS Development Environment) software
 * CAVE (Common AWIPS Visualization Environment) software
 * EDEX (Environmental Data Exchange) software
 * uFrame™ (Universal Framework) software
 *
 * Copyright (c) 2010 Raytheon Co.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/epl-v10.php
 *
 *
 * Contractor Name: Raytheon Company
 * Contractor Address:
 * 6825 Pine Street, Suite 340
 * Mail Stop B8
 * Omaha, NE 68106
 * 402.291.0100
 *
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 22, 2011            bclement     Initial creation
 *
 */
package com.raytheon.uf.edex.wfs.filter.v2_0_0;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;

import net.opengis.filter.v_2_0_0.FunctionType;
import net.opengis.filter.v_2_0_0.LiteralType;

/**
 * Visitor pattern support classes for filter expression operations
 * 
 * @author bclement
 * @version 1.0
 */
public abstract class AbsExpressionOp {

    /**
     * @param element
     * @param visitor
     * @param obj
     * @return
     * @throws Exception
     */
    public abstract Object visit(JAXBElement<?> element,
            IExpressionVisitor visitor, Object obj) throws Exception;

    /**
     * processes literal values
     */
    public static class Literal extends AbsExpressionOp {
        @Override
        public Object visit(JAXBElement<?> element, IExpressionVisitor visitor,
                Object obj) throws Exception {
            LiteralType literal = (LiteralType) element.getValue();
            return visitor.literal(literal.getContent(), obj);
        }
    }

    /**
     * processes value references
     */
    public static class ValueRef extends AbsExpressionOp {
        @Override
        public Object visit(JAXBElement<?> element, IExpressionVisitor visitor,
                Object obj) throws Exception {
            Object value = element.getValue();
            return visitor.valueRef(value.toString(), obj);
        }

    }

    /**
     * processes custom functions
     */
    public static class Function extends AbsExpressionOp {
        @Override
        public FilterFunction visit(JAXBElement<?> element,
                IExpressionVisitor visitor, Object obj) throws Exception {
            FunctionType f = (FunctionType) element.getValue();
            String name = f.getName();
            List<JAXBElement<?>> exprs = f.getExpression();
            List<ExpressionProcessor> procs = new ArrayList<ExpressionProcessor>(
                    exprs.size());
            for (JAXBElement<?> expr : exprs) {
                procs.add(new ExpressionProcessor(expr));
            }
            return visitor.function(procs, name, obj);
        }
    }
}