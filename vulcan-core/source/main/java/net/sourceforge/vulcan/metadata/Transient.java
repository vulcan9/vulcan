/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2011 Chris Eldredge
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sourceforge.vulcan.metadata;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is intended to be used to indicate to a persistence layer that
 * a field on a Data Transfer Object should not be persisted.  Place this annotation
 * on the getter method for the field which should be skipped during persistence.
 * <br><br>
 * Implementation Note:  It probably makes more sense to apply this annotation to the field
 * itself; however this proves to be more difficult in implementation since encapsulation
 * dictates that fields should not be publicly accessible.  In order to comply with both,
 * an introspecting object would have to gain access to private fields on a DTO in order
 * to gain access to the corresponding annotations.  Therefore applying the annotation to
 * the getter method is chosen as a compromise.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Transient {
}
