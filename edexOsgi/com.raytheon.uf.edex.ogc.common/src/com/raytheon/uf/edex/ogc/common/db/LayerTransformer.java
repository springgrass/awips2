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
package com.raytheon.uf.edex.ogc.common.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang.time.DateUtils;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.dataquery.db.QueryParam;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.database.dao.CoreDao;
import com.raytheon.uf.edex.database.dao.DaoConfig;
import com.raytheon.uf.edex.database.query.DatabaseQuery;
import com.raytheon.uf.edex.ogc.common.OgcBoundingBox;
import com.raytheon.uf.edex.ogc.common.OgcDimension;
import com.raytheon.uf.edex.ogc.common.OgcGeoBoundingBox;
import com.raytheon.uf.edex.ogc.common.OgcLayer;
import com.raytheon.uf.edex.ogc.common.OgcStyle;
import com.raytheon.uf.edex.ogc.common.StyleLookup;
import com.vividsolutions.jts.geom.Polygon;

/**
 *
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 13, 2011            bclement     Initial creation
 *
 **/

public class LayerTransformer<DIMENSION extends SimpleDimension, L extends SimpleLayer<DIMENSION>> {

    protected static IUFStatusHandler log = UFStatus
            .getHandler(LayerTransformer.class);

	public enum TimeFormat {
		LIST, HOUR_RANGES
	};

	protected String key;

	protected static String timeUnit = "ISO8601";

    protected Class<L> layerClass;

	public static final Pattern frontDot = Pattern
			.compile("^(-?[0-9]*\\.?[0-9]+)(.*)$");
	public static final Pattern backDot = Pattern
			.compile("^(-?[0-9]+\\.?[0-9]*)(.*)$");

	/**
	 * Construct a LayerTransformer that uses a different layerClass and
	 * layerWrapperClass than the default SimpleLayer and LayerHolder
	 * 
	 * @param key
	 * @param layerTable
	 * @param das
	 * @param layerClass
	 * @param layerHolderClass
	 */
    public LayerTransformer(String key, Class<L> layerClass) {
		this.key = key;
		this.layerClass = layerClass;
	}

	/**
	 * @param name
	 * @return null if layer not found
	 * @throws DataAccessLayerException
	 */
    public L find(String name) throws DataAccessLayerException {
		String field = "name";
		List<QueryParam> params = Arrays.asList(new QueryParam(field, name));
        List<L> res;
		res = query(layerClass, params);
		if (res == null || res.isEmpty()) {
			return null;
		}
		if (res.size() > 1) {
			log.warn("Multiple layers found with the same name, returning first");
		}
		return layerClass.cast(res.get(0));
	}

	/**
	 * @param layer
	 * @param dimension
	 * @return null if layer/dimension not found
	 * @throws DataAccessLayerException
	 */
	public SimpleDimension getDimension(String layer, String dimension)
			throws DataAccessLayerException {
        SimpleLayer<?> l = find(layer);
		return getDimension(l, dimension);
	}

	/**
	 * @param layer
	 * @param dimension
	 * @return null if layer/dimension not found
	 */
    public static <DIMENSION extends SimpleDimension, L extends SimpleLayer<DIMENSION>> DIMENSION getDimension(
            L layer,
			String dimension) {
		if (layer == null) {
			return null;
		}
        DIMENSION rval = null;
        for (DIMENSION d : layer.getDimensions()) {
			if (d.getName().equalsIgnoreCase(dimension)) {
				rval = d;
				break;
			}
		}
		return rval;
	}

	/**
	 * @param dim
	 * @return empty set if dim is null or doesn't have any parsable values
	 */
	public static TreeSet<Double> getDimValuesAsDouble(SimpleDimension dim) {
		TreeSet<Double> rval = new TreeSet<Double>();
		if (dim == null) {
			return rval;
		}
		for (String val : dim.getValues()) {
			try {
				Matcher m = frontDot.matcher(val);
				if (m.matches()) {
					val = m.group(1);
				} else {
					m = backDot.matcher(val);
					if (m.matches()) {
						val = m.group(1);
					}
				}
				rval.add(Double.parseDouble(val));
			} catch (Throwable e) {
				// continue
			}
		}
		return rval;
	}

	/**
	 * @param layer
	 * @return
	 */
    public List<OgcBoundingBox> getBoundingBoxes(L layer) {
		String crs = layer.getTargetCrsCode();
		double minx = layer.getTargetMinx();
		double maxx = layer.getTargetMaxx();
		double miny = layer.getTargetMiny();
		double maxy = layer.getTargetMaxy();
		OgcBoundingBox rval = new OgcBoundingBox(crs, minx, maxx, miny, maxy);
		return Arrays.asList(rval);
	}

	public static String getCrsName(CoordinateReferenceSystem crs) {
		if (crs == null) {
			return null;
		}
		Set<ReferenceIdentifier> ids = crs.getIdentifiers();
		if (ids == null || ids.isEmpty()) {
			return crs.getName().toString();
		} else {
			return ids.iterator().next().toString();
		}
	}

	/**
	 * @param layer
	 * @return
	 */
    public static <L extends SimpleLayer<?>> OgcGeoBoundingBox getGeoBoundingBox(
            L layer) {
		Polygon crs84Bounds = layer.getCrs84Bounds();
		if (crs84Bounds == null) {
			return null;
		}
		ReferencedEnvelope env = JTS.toEnvelope(crs84Bounds);
		return new OgcGeoBoundingBox(env);
	}

	/**
	 * @param layer
	 * @param tformat
	 * @return
	 */
    public static <L extends SimpleLayer<?>> List<String> getTimes(L layer) {
		return getTimes(layer, TimeFormat.LIST);
	}

	/**
	 * @param layer
	 * @param tformat
	 * @return
	 */
    public static <L extends SimpleLayer<?>> List<String> getTimes(L layer,
            TimeFormat tformat) {
		List<String> rval;
		// TODO this could be adapted to a pattern that allows for formatters to
		// be externally added
		switch (tformat) {
		case LIST:
			rval = getTimesAsList(layer);
			break;
		case HOUR_RANGES:
			rval = getTimesAsHourRanges(layer);
			break;
		default:
			throw new IllegalArgumentException("No handler for time format: "
					+ tformat);
		}
		return rval;
	}

	/**
	 * @param layer
	 * @return
	 */
    protected static <L extends SimpleLayer<?>> List<String> getTimesAsHourRanges(
            L layer) {
		Set<Date> times = layer.getTimes();
		if (times == null || times.isEmpty()) {
			return new ArrayList<String>(0);
		}
		Set<Date> set = new TreeSet<Date>();
		set.addAll(times);
		Iterator<Date> i = set.iterator();
		StringBuilder sb = new StringBuilder();
		List<String> rval = new ArrayList<String>();
		Date curr = i.next();
		while (curr != null) {
			startRange(sb, curr);
			curr = endRange(curr, i, sb);
			if (curr != null) {
				rval.add(sb.toString());
				sb = new StringBuilder();
			}
		}
		String last = sb.toString();
		if (!last.isEmpty()) {
			rval.add(last);
		}
		return rval;
	}

	/**
	 * End a range started by startRange()
	 * 
	 * @param d
	 *            start of range
	 * @param i
	 *            iterator to rest of times that possibly include the rest of
	 *            the range
	 * @param sb
	 *            where the formatted output goes
	 * @return the start of the next range, null if there are no more ranges
	 */
	protected static Date endRange(Date d, Iterator<Date> i, StringBuilder sb) {
		long milliStart = d.getTime();
		long milliPrev = milliStart;
		Date rval = null;
		Date prev = null;
		Date curr = null;
		while (i.hasNext()) {
			curr = i.next();
            if (curr.getTime() - milliPrev > TimeUtil.MILLIS_PER_HOUR) {
				// we've reached the end of the range return rval
				rval = curr;
				break;
			}
			prev = curr;
			milliPrev = prev.getTime();
		}
		if (prev == null) {
			// iterator didn't have anything
            prev = new Date(milliStart + TimeUtil.MILLIS_PER_HOUR);
		} else {
			// we want the range to end at the next hour
            prev = new Date(prev.getTime() + TimeUtil.MILLIS_PER_HOUR);
		}
		sb.append(format(prev));
		// FIXME 0 indicates a continuum range, we should support discrete
		// periods in the range
		sb.append("/0");
		return rval;
	}

	public static String format(Date d) {
		Calendar c = GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"));
		c.setTime(d);
		return DatatypeConverter.printDateTime(c);
	}

	protected static void startRange(StringBuilder sb, Date d) {
		sb.append(format(d));
		sb.append('/');
	}

    protected static <L extends SimpleLayer<?>> List<String> getTimesAsList(
            L layer) {
		Set<Date> times = layer.getTimes();
		if (times == null || times.isEmpty()) {
			return new ArrayList<String>(0);
		}
		TreeSet<Date> sorted = new TreeSet<Date>(times);
		Iterator<Date> i = sorted.iterator();
		List<String> rval = new ArrayList<String>(sorted.size());
		while (i.hasNext()) {
			rval.add(format(i.next()));
		}
		return rval;
	}

	public TreeSet<Date> getAllTimes() {
		TreeSet<Date> rval = new TreeSet<Date>();
        List<L> layers = getLayers();
        for (L l : layers) {
			rval.addAll(l.getTimes());
		}
		return rval;
	}

    public List<L> getLayers() {
        List<L> rval = query(layerClass);
		return rval;
	}

	public List<OgcLayer> getLayersAsOgc(TimeFormat tformat, StyleLookup lookup) {
		return transform(getLayers(), tformat, lookup);
	}

	public List<OgcLayer> getLayersAsOgc(StyleLookup lookup) {
		return getLayersAsOgc(TimeFormat.LIST, lookup);
	}

	/**
	 * @param layers
	 * @return
	 */
    protected List<OgcLayer> transform(List<L> layers,
			TimeFormat tformat, StyleLookup lookup) {
		List<OgcLayer> rval = new ArrayList<OgcLayer>(layers.size());
        for (L simple : layers) {
			rval.add(transform(simple, tformat, lookup));
		}
		return rval;
	}

	/**
	 * Transform a simple layer as represented in the data storage area to an
	 * OgcLayer that can be viewed through getCapabilities
	 * <p>
	 * Override this method to add additional Dimensions.
	 * 
	 * @param layer
	 * @return
	 */
    public OgcLayer transform(L layer, TimeFormat tformat,
			StyleLookup lookup) {
		OgcLayer rval = new OgcLayer();
		String name = layer.getName();
		rval.setName(key, name);
		rval.setTitle(name);
		setStylesForLayer(rval.getName(), layer, rval, lookup);
		OgcDimension timeDim = new OgcDimension("time", timeUnit, getTimes(layer,
				tformat));
		Date time = layer.getDefaultTime();
		String defaultTime;
		if (tformat.equals(TimeFormat.HOUR_RANGES)) {
			defaultTime = getTimeRange(time);
		} else {
			defaultTime = format(time);
		}
		timeDim.setDefaultVal(defaultTime);
		rval.addDimension(timeDim);
		for (OgcDimension dim : getDims(layer, layer.getDimensions())) {
			rval.addDimension(dim);
		}
		rval.setGeoBoundingBox(getGeoBoundingBox(layer));
		rval.setBoundingBox(getBoundingBoxes(layer));
		// rval.setCrs(Arrays.asList(layer.getTargetCrs()));
		return rval;
	}

	public String getTimeRange(Date d) {
		Date start = DateUtils.truncate(d, Calendar.HOUR);
		Date end = DateUtils.addHours(start, 1);
		return format(start) + '/' + format(end);
	}

    protected void setStylesForLayer(String layerName, L layer,
			OgcLayer rval, StyleLookup lookup) {
		if (lookup != null) {
			String style = lookup.lookup(layerName);
			if (style != null) {
				OgcStyle ogcstyle = new OgcStyle(style);
				String url = "&layer=" + layerName + "&style=" + style
						+ "&format=image/png";
				ogcstyle.setLegendUrl(url.replaceAll(" ", "%20"));
				ogcstyle.setDefault(true);
				rval.setStyles(Arrays.asList(ogcstyle));
			}
		}
	}

	/**
	 * @param layer
	 * @return
	 */
    protected OgcDimension[] getDims(L layer, Set<DIMENSION> dims) {
		if (dims == null) {
			return new OgcDimension[0];
		}
        List<OgcDimension> rval = new ArrayList<OgcDimension>(dims.size());
        for (DIMENSION dim : dims) {
			OgcDimension ogcdim = transform(layer, dim);
			if (ogcdim != null) {
				rval.add(ogcdim);
			}
		}
		return rval.toArray(new OgcDimension[rval.size()]);
	}

	/**
	 * @param simpleDimension
	 * @return
	 */
    protected OgcDimension transform(L layer, DIMENSION dim) {
		if (dim == null || dim.getName() == null || dim.getValues() == null) {
			return null;
		}
		OgcDimension rval;
		List<String> values = new ArrayList<String>(dim.getValues());
		if (dim.getUnits() == null) {
			rval = new OgcDimension(dim.getName(), values);
		} else {
			rval = new OgcDimension(dim.getName(), dim.getUnits(), values);
		}
		rval.setDefaultVal(dim.getDefaultValue(layer));
		return rval;
	}

    protected OgcLayer transform(L layer, StyleLookup lookup) {
		return transform(layer, TimeFormat.LIST, lookup);
	}

    protected <T> List<T> query(Class<L> c) {
		return query(c, null);
	}

    protected <T> List<T> query(Class<L> c, List<QueryParam> params) {
		try {
			DatabaseQuery query = new DatabaseQuery(c);
			if (params != null && !params.isEmpty()) {
				for (QueryParam p : params) {
					query.addQueryParam(p);
				}
			}
			DaoConfig config = DaoConfig.forClass(layerClass);
			CoreDao dao = new CoreDao(config);
            @SuppressWarnings("unchecked")
            final List<T> retVal = (List<T>) dao.queryByCriteria(query);
            return retVal;
		} catch (Exception e) {
			log.error("Unable to query entity: " + c, e);
			return new ArrayList<T>(0);
		}
	}

    protected <H extends LayerHolder<L>> List<L> unwrap(
			List<H> holders) {
        List<L> rval = new ArrayList<L>(holders.size());
		for (H holder : holders) {
			rval.add(holder.getValue());
		}
		return rval;
	}

	/**
	 * @param layerName
	 * @return null if layer name isn't found
	 * @throws DataAccessLayerException
	 */
	public Date getLatestTime(String layerName) throws DataAccessLayerException {
        L simpleLayer = find(layerName);
		return getLatestTime(simpleLayer);
	}

	/**
	 * @param layer
	 * @return null if layer name isn't found
	 */
    public static <L extends SimpleLayer<?>> Date getLatestTime(L layer) {
		if (layer == null) {
			return null;
		}
		Set<Date> times = layer.getTimes();
		TreeSet<Date> sorted = new TreeSet<Date>(times);
		return sorted.last();
	}

	/**
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * @param key
	 *            the key to set
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * @param layerClass
	 *            the layerClass to set
	 */
    public void setLayerClass(Class<L> layerClass) {
		this.layerClass = layerClass;
	}

	/**
	 * @return the layerClass
	 */
    public Class<L> getLayerClass() {
		return layerClass;
	}

	/**
	 * @return
	 */
	public TreeSet<Date> getLatestTimes() {
        List<L> layers = getLayers();
		TreeSet<Date> rval = new TreeSet<Date>();
        for (L l : layers) {
			TreeSet<Date> times = new TreeSet<Date>(l.getTimes());
			rval.add(times.last());
		}
		return rval;
	}

}