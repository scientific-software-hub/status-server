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

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import wpn.hdri.ss.data.Interpolation;
import wpn.hdri.ss.data.Method;

import javax.annotation.concurrent.Immutable;
import java.math.BigDecimal;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 26.04.12
 */
@Immutable
@Element(name = "attribute")
public final class DeviceAttribute {
    @Attribute(name = "name")
    private final String name;
    @Attribute(name = "method")
    private final Method method;
    @Attribute(name = "interpolation")
    private final Interpolation interpolation;
    @Attribute(name = "delay")
    private final long delay;
    @Attribute(name = "precision", required = false)
    private final BigDecimal precision;

    public DeviceAttribute(
            @Attribute(name = "name") String name,
            @Attribute(name = "method") Method method,
            @Attribute(name = "interpolation") Interpolation interpolation,
            @Attribute(name = "delay") long delay,
            @Attribute(name = "precision") BigDecimal precision
    ) {
        if (method == Method.EVENT) {
            Preconditions.checkArgument(delay == 0, "For event attributes delay should be equal to 0");
        }
        if (method == Method.POLL) {
            Preconditions.checkArgument(delay >= 20, "For poll attributes delay should be greater or equal to 20");
        }
        this.name = name;
        this.method = method;
        this.interpolation = interpolation;
        this.delay = delay;
        this.precision = precision;
    }

    public DeviceAttribute(
            @Attribute(name = "name") String name,
            @Attribute(name = "method") Method method,
            @Attribute(name = "interpolation") Interpolation interpolation,
            @Attribute(name = "delay") long delay
    ) {
        this(name, method, interpolation, delay, BigDecimal.ZERO);
    }

    public String getName() {
        return name;
    }

    public Method getMethod() {
        return method;
    }

    public Interpolation getInterpolation() {
        return interpolation;
    }

    public long getDelay() {
        return delay;
    }

    public BigDecimal getPrecision() {
        return precision;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("name", name)
                .add("method", method)
                .add("interpolation", interpolation)
                .add("delay", delay)
                .toString();
    }
}
