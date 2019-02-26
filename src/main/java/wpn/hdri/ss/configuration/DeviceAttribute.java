/*
 * The main contributor to this project is Institute of Materials Research,
 * Helmholtz-Zentrum Geesthacht,
 * Germany.
 *
 * This project is a contribution of the Helmholtz Association Centres and
 * Technische Universitaet Muenchen to the ESS Design Update Phase.
 *
 * The project's funding reference is FKZ05E11CG1.
 *
 * Copyright (c) 2012. Institute of Materials Research,
 * Helmholtz-Zentrum Geesthacht,
 * Germany.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package wpn.hdri.ss.configuration;


import com.google.common.base.Preconditions;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Validate;
import wpn.hdri.ss.data.Method;

import java.math.BigDecimal;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 26.04.12
 */
@Element(name = "attribute")
public final class DeviceAttribute {
    @Attribute(name = "name")
    private String name;
    @Attribute(name = "alias", required = false)
    private String alias;
    @Attribute(name = "method")
    private Method method;
    @Attribute(name = "interpolation")
    private String interpolation;
    @Attribute(name = "delay", required = false)
    private long delay;
    @Attribute(name = "precision", required = false)
    private BigDecimal precision;
    @Attribute(name = "type", required = false)
    private String eventType;


//        if (method == Method.EVENT) {
//            Preconditions.checkArgument(delay == 0, "For event attributes delay should be equal to 0");
//        }
//        if (method == Method.POLL) {
//            Preconditions.checkArgument(delay >= 20, "For poll attributes delay should be greater or equal to 20");
//        }

    @Validate
    public void validate(){
        if (this.method == Method.POLL && this.delay > 0) {
            Preconditions.checkArgument(this.delay >= 20,"polling delay should be greater than 20");
        }
        if (this.method == Method.EVENT) {
            Preconditions.checkArgument(eventType != null && !eventType.isEmpty(), "Please specify type attribute when method = event");
        }
        if(this.precision == null){
            this.precision = BigDecimal.ZERO;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlias(){
        return alias == null ? name : alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public String getInterpolation() {
        return interpolation;
    }

    public void setInterpolation(String interpolation) {
        this.interpolation = interpolation;
    }

    public long getDelay() {
        if (method == Method.POLL && delay > 0)
            return delay;
        else if (method == Method.POLL)
            return 3000L;
        else
            return 0L;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public BigDecimal getPrecision() {
        return precision == null ? BigDecimal.ZERO : precision;
    }

    public void setPrecision(BigDecimal precision) {
        this.precision = precision;
    }

    /**
     *
     *
     * @return NONE ar event type specified in xml
     */
    public String getEventType() {
        return eventType == null ? "NONE" : eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + hashCode() + "{" +
                "name=" + name +
                ";alias=" + alias +
                ";method=" + method +
                ";interpolation=" + interpolation +
                ";delay=" + delay +
                "}";
    }
}
