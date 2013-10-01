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
//TODO move into a dedicated project. The new project will serve as a single entry point for all storing tasks
//TODO implement annotations
package wpn.hdri.ss.storage;

/**
 * This is a very rough approach for storing data. Basically it takes header (a list of Strings) and data (a list of list of Strings) and a data name
 * Data name is a "table name".
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 27.04.12
 */
public interface Storage {
    void save(String dataName, Iterable<String> header, Iterable<Iterable<String>> body) throws StorageException;

    <T> Iterable<T> load(String dataName, TypeFactory<T> factory) throws StorageException;

    void delete(String dataName) throws StorageException;
}
