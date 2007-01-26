/**
 *******************************************************************************
 * Copyright (C) 2001-2006, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.ibm.icu.util.ULocale;

public class ICULocaleService extends ICUService {
    private ULocale fallbackLocale;
    private String fallbackLocaleName;

    /**
     * Construct an ICULocaleService.
     */
    public ICULocaleService() {
    }

    /**
     * Construct an ICULocaleService with a name (useful for debugging).
     */
    public ICULocaleService(String name) {
        super(name);
    }

    /**
     * Convenience override for callers using locales.  This calls
     * get(ULocale, int, ULocale[]) with KIND_ANY for kind and null for
     * actualReturn.
     */
    public Object get(ULocale locale) {
        return get(locale, LocaleKey.KIND_ANY, null);
    }

    /**
     * Convenience override for callers using locales.  This calls
     * get(ULocale, int, ULocale[]) with a null actualReturn.
     */
    public Object get(ULocale locale, int kind) {
        return get(locale, kind, null);
    }

    /**
     * Convenience override for callers using locales.  This calls
     * get(ULocale, int, ULocale[]) with KIND_ANY for kind.
     */
    public Object get(ULocale locale, ULocale[] actualReturn) {
        return get(locale, LocaleKey.KIND_ANY, actualReturn);
    }

    /**
     * Convenience override for callers using locales.  This uses
     * createKey(ULocale.toString(), kind) to create a key, calls getKey, and then
     * if actualReturn is not null, returns the actualResult from
     * getKey (stripping any prefix) into a ULocale.  
     */
    public Object get(ULocale locale, int kind, ULocale[] actualReturn) {
        Key key = createKey(locale, kind);
        if (actualReturn == null) {
            return getKey(key);
        }

        String[] temp = new String[1];
        Object result = getKey(key, temp);
        if (result != null) {
            int n = temp[0].indexOf("/");
            if (n >= 0) {
                temp[0] = temp[0].substring(n+1);
            }
            actualReturn[0] = new ULocale(temp[0]);
        }
        return result;
    }

    /**
     * Convenience override for callers using locales.  This calls
     * registerObject(Object, ULocale, int kind, boolean visible)
     * passing KIND_ANY for the kind, and true for the visibility.
     */
    public Factory registerObject(Object obj, ULocale locale) {
        return registerObject(obj, locale, LocaleKey.KIND_ANY, true);
    }

    /**
     * Convenience override for callers using locales.  This calls
     * registerObject(Object, ULocale, int kind, boolean visible)
     * passing KIND_ANY for the kind.
     */
    public Factory registerObject(Object obj, ULocale locale, boolean visible) {
        return registerObject(obj, locale, LocaleKey.KIND_ANY, visible);
    }

    /**
     * Convenience function for callers using locales.  This calls
     * registerObject(Object, ULocale, int kind, boolean visible)
     * passing true for the visibility.
     */
    public Factory registerObject(Object obj, ULocale locale, int kind) {
        return registerObject(obj, locale, kind, true);
    }

    /**
     * Convenience function for callers using locales.  This  instantiates
     * a SimpleLocaleKeyFactory, and registers the factory.
     */
    public Factory registerObject(Object obj, ULocale locale, int kind, boolean visible) {
        Factory factory = new SimpleLocaleKeyFactory(obj, locale, kind, visible);
        return registerFactory(factory);
    }

    /**
     * Convenience method for callers using locales.  This returns the standard
     * Locale list, built from the Set of visible ids.
     */
    public Locale[] getAvailableLocales() {
        // TODO make this wrap getAvailableULocales later
        Set visIDs = getVisibleIDs();
        Iterator iter = visIDs.iterator();
        Locale[] locales = new Locale[visIDs.size()];
        int n = 0;
        while (iter.hasNext()) {
            Locale loc = LocaleUtility.getLocaleFromName((String)iter.next());
            locales[n++] = loc;
        }
        return locales;
    }

    /**
     * Convenience method for callers using locales.  This returns the standard
     * ULocale list, built from the Set of visible ids.
     */
    public ULocale[] getAvailableULocales() {
        Set visIDs = getVisibleIDs();
        Iterator iter = visIDs.iterator();
        ULocale[] locales = new ULocale[visIDs.size()];
        int n = 0;
        while (iter.hasNext()) {
            locales[n++] = new ULocale((String)iter.next());
        }
        return locales;
    }
        
    /**
     * A subclass of Key that implements a locale fallback mechanism.
     * The first locale to search for is the locale provided by the
     * client, and the fallback locale to search for is the current
     * default locale.  If a prefix is present, the currentDescriptor
     * includes it before the locale proper, separated by "/".  This
     * is the default key instantiated by ICULocaleService.</p>
     *
     * <p>Canonicalization adjusts the locale string so that the
     * section before the first understore is in lower case, and the rest
     * is in upper case, with no trailing underscores.</p> 
     */
    public static class LocaleKey extends ICUService.Key {
        private int kind;
        private int varstart;
        private String primaryID;
        private String fallbackID;
        private String currentID;

        public static final int KIND_ANY = -1;

        /**
         * Create a LocaleKey with canonical primary and fallback IDs.
         */
        public static LocaleKey createWithCanonicalFallback(String primaryID, String canonicalFallbackID) {
            return createWithCanonicalFallback(primaryID, canonicalFallbackID, KIND_ANY);
        }
            
        /**
         * Create a LocaleKey with canonical primary and fallback IDs.
         */
        public static LocaleKey createWithCanonicalFallback(String primaryID, String canonicalFallbackID, int kind) {
            if (primaryID == null) {
                return null;
            }
            if (primaryID.length() == 0) {
                primaryID = "root";
            }
            String canonicalPrimaryID = ULocale.getName(primaryID);
            return new LocaleKey(primaryID, canonicalPrimaryID, canonicalFallbackID, kind);
        }
            
        /**
         * Create a LocaleKey with canonical primary and fallback IDs.
         */
        public static LocaleKey createWithCanonical(ULocale locale, String canonicalFallbackID, int kind) {
            if (locale == null) {
                return null;
            }
            String canonicalPrimaryID = locale.getName();
            return new LocaleKey(canonicalPrimaryID, canonicalPrimaryID, canonicalFallbackID, kind);
        }
            
        /**
         * PrimaryID is the user's requested locale string,
         * canonicalPrimaryID is this string in canonical form,
         * fallbackID is the current default locale's string in
         * canonical form.
         */
        protected LocaleKey(String primaryID, String canonicalPrimaryID, String canonicalFallbackID, int kind) {
            super(primaryID);

            this.kind = kind;
            if (canonicalPrimaryID == null) {
                this.primaryID = "";
            } else {
                this.primaryID = canonicalPrimaryID;
                this.varstart = this.primaryID.indexOf('@');
            }
            if (this.primaryID == "") {
                this.fallbackID = null;
            } else {
                if (canonicalFallbackID == null || this.primaryID.equals(canonicalFallbackID)) {
                    this.fallbackID = "";
                } else {
                    this.fallbackID = canonicalFallbackID;
                }
            }

            this.currentID = varstart == -1 ? this.primaryID : this.primaryID.substring(0, varstart);
        }

        /**
         * Return the prefix associated with the kind, or null if the kind is KIND_ANY.
         */
        public String prefix() {
            return kind == KIND_ANY ? null : Integer.toString(kind());
        }

        /**
         * Return the kind code associated with this key.
         */
        public int kind() {
            return kind;
        }

        /**
         * Return the (canonical) original ID.
         */
        public String canonicalID() {
            return primaryID;
        }

        /**
         * Return the (canonical) current ID, or null if no current id.
         */
        public String currentID() {
            return currentID;
        }

        /**
         * Return the (canonical) current descriptor, or null if no current id.
         * Includes the keywords, whereas the ID does not include keywords.
         */
        public String currentDescriptor() {
            String result = currentID();
            if (result != null) {
                StringBuffer buf = new StringBuffer(result.length() + 32);
                if (kind != KIND_ANY) {
                    buf.append(prefix());
                }
                buf.append('/');
                buf.append(result);
                if (varstart != -1) {
                    buf.append(primaryID.substring(varstart, primaryID.length()));
                }
                result = buf.toString();
            }
            return result;
        }

        /**
         * Convenience method to return the locale corresponding to the (canonical) original ID.
         */
        public ULocale canonicalLocale() {
            return new ULocale(primaryID);
        }

        /**
         * Convenience method to return the ulocale corresponding to the (canonical) currentID.
         */
        public ULocale currentLocale() {
            if (varstart == -1) {
                return new ULocale(currentID);
            } else {
                return new ULocale(currentID + primaryID.substring(varstart));
            }
        }

        /**
         * If the key has a fallback, modify the key and return true,
         * otherwise return false.</p>
         *
         * <p>First falls back through the primary ID, then through
         * the fallbackID.  The final fallback is "root"
         * unless the primary id was "root", in which case
         * there is no fallback.  
         */
        public boolean fallback() {
            int x = currentID.lastIndexOf('_');
            if (x != -1) {
                while (--x >= 0 && currentID.charAt(x) == '_') { // handle zh__PINYIN
                }
                currentID = currentID.substring(0, x+1);
                return true;
            }
            if (fallbackID != null) {
                if (fallbackID.length() == 0) {
                    currentID = "root";
                    fallbackID = null;
                } else {
                    currentID = fallbackID;
                    fallbackID = "";
                }
                return true;
            }
            currentID = null;
            return false;
        }

        /**
         * If a key created from id would eventually fallback to match the 
         * canonical ID of this key, return true.
         */
        public boolean isFallbackOf(String id) {
            return LocaleUtility.isFallbackOf(canonicalID(), id);
        }
    }

    /**
     * A subclass of Factory that uses LocaleKeys.  If 'visible' the
     * factory reports its IDs.
     */
    public static abstract class LocaleKeyFactory implements Factory {
        protected final String name;
        protected final boolean visible;

        public static final boolean VISIBLE = true;
        public static final boolean INVISIBLE = false;

        /**
         * Constructor used by subclasses.
         */
        protected LocaleKeyFactory(boolean visible) {
            this.visible = visible;
            this.name = null;
        }

        /**
         * Constructor used by subclasses.
         */
        protected LocaleKeyFactory(boolean visible, String name) {
            this.visible = visible;
            this.name = name;
        }

        /**
         * Implement superclass abstract method.  This checks the currentID of
         * the key against the supported IDs, and passes the canonicalLocale and
         * kind off to handleCreate (which subclasses must implement).
         */
        public Object create(Key key, ICUService service) {
            if (handlesKey(key)) {
                LocaleKey lkey = (LocaleKey)key;
                int kind = lkey.kind();
                
                ULocale uloc = lkey.currentLocale();
                return handleCreate(uloc, kind, service);
            } else {
                // System.out.println("factory: " + this + " did not support id: " + key.currentID());
                // System.out.println("supported ids: " + getSupportedIDs());
            }
            return null;
        }

        protected boolean handlesKey(Key key) {
            if (key != null) {
                String id = key.currentID();
                Set supported = getSupportedIDs();
                return supported.contains(id);
            }
            return false;
        }

        /**
         * Override of superclass method.
         */
        public void updateVisibleIDs(Map result) {
            Set cache = getSupportedIDs();
            Iterator iter = cache.iterator();
            while (iter.hasNext()) {
                String id = (String)iter.next();
                if (visible) {
                    result.put(id, this);
                } else {
                    result.remove(id);
                }
            }
        }

        /**
         * Return a localized name for the locale represented by id.
         */
        public String getDisplayName(String id, ULocale locale) {
            // assume if the user called this on us, we must have handled some fallback of this id
            //          if (isSupportedID(id)) {
            if (locale == null) {
                return id;
            }
            ULocale loc = new ULocale(id);
            return loc.getDisplayName(locale);
            //              }
            //          return null;
        }

        ///CLOVER:OFF
        /**
         * Utility method used by create(Key, ICUService).  Subclasses can
         * implement this instead of create.
         */
        protected Object handleCreate(ULocale loc, int kind, ICUService service) {
            return null;
        }
        ///CLOVER:ON

        /**
         * Return true if this id is one the factory supports (visible or 
         * otherwise).
         */
        protected boolean isSupportedID(String id) {
            return getSupportedIDs().contains(id);
        }
        
        /**
         * Return the set of ids that this factory supports (visible or 
         * otherwise).  This can be called often and might need to be
         * cached if it is expensive to create.
         */
        protected Set getSupportedIDs() {
            return Collections.EMPTY_SET;
        }

        /**
         * For debugging.
         */
        public String toString() {
            StringBuffer buf = new StringBuffer(super.toString());
            if (name != null) {
                buf.append(", name: ");
                buf.append(name);
            }
            buf.append(", visible: ");
            buf.append(visible);
            return buf.toString();
        }
    }

    /**
     * A LocaleKeyFactory that just returns a single object for a kind/locale.
     */
    public static class SimpleLocaleKeyFactory extends LocaleKeyFactory {
        private final Object obj;
        private final String id;
        private final int kind;

        // TODO: remove when we no longer need this
        public SimpleLocaleKeyFactory(Object obj, ULocale locale, int kind, boolean visible) {
            this(obj, locale, kind, visible, null);
        }

        public SimpleLocaleKeyFactory(Object obj, ULocale locale, int kind, boolean visible, String name) {
            super(visible, name);
            
            this.obj = obj;
            this.id = locale.getBaseName();
            this.kind = kind;
        }

        /**
         * Returns the service object if kind/locale match.  Service is not used.
         */
        public Object create(Key key, ICUService service) {
            LocaleKey lkey = (LocaleKey)key;
            if (kind == LocaleKey.KIND_ANY || kind == lkey.kind()) {
                String keyID = lkey.currentID();
                if (id.equals(keyID)) {
                    return obj;
                }
            }
            return null;
        }

        protected boolean isSupportedID(String id) {
            return this.id.equals(id);
        }

        public void updateVisibleIDs(Map result) {
            if (visible) {
                result.put(id, this);
            } else {
                result.remove(id);
            }
        }

        public String toString() {
            StringBuffer buf = new StringBuffer(super.toString());
            buf.append(", id: ");
            buf.append(id);
            buf.append(", kind: ");
            buf.append(kind);
            return buf.toString();
        }
    }

    /**
     * A LocaleKeyFactory that creates a service based on the ICU locale data.
     * This is a base class for most ICU factories.  Subclasses instantiate it
     * with a constructor that takes a bundle name, which determines the supported
     * IDs.  Subclasses then override handleCreate to create the actual service
     * object.  The default implementation returns a resource bundle.
     */
    public static class ICUResourceBundleFactory extends LocaleKeyFactory {
        protected final String bundleName;

        /**
         * Convenience constructor that uses the main ICU bundle name.
         */
        public ICUResourceBundleFactory() {
            this(ICUResourceBundle.ICU_BASE_NAME);
        }

        /**
         * A service factory based on ICU resource data in resources
         * with the given name.
         */
        public ICUResourceBundleFactory(String bundleName) {
            super(true);

            this.bundleName = bundleName;
        }

        /**
         * Return the supported IDs.  This is the set of all locale names for the bundleName.
         */
        protected Set getSupportedIDs() {
            // note: "root" is one of the ids, but "" is not.  Must convert ULocale.ROOT.
            return ICUResourceBundle.getFullLocaleNameSet(bundleName); 
        }

        /**
         * Override of superclass method.
         */
        public void updateVisibleIDs(Map result) {
            Set visibleIDs = ICUResourceBundle.getAvailableLocaleNameSet(bundleName); // only visible ids
            Iterator iter = visibleIDs.iterator();
            while (iter.hasNext()) {
                String id = (String)iter.next();
                result.put(id, this);
            }
        }

        /**
         * Create the service.  The default implementation returns the resource bundle
         * for the locale, ignoring kind, and service.
         */
        protected Object handleCreate(ULocale loc, int kind, ICUService service) {
            return ICUResourceBundle.getBundleInstance(bundleName, loc);
        }

        public String toString() {
            return super.toString() + ", bundle: " + bundleName;
        }
    }

    /**
     * Return the name of the current fallback locale.  If it has changed since this was
     * last accessed, the service cache is cleared.
     */
    public String validateFallbackLocale() {
        ULocale loc = ULocale.getDefault();
        if (loc != fallbackLocale) {
            synchronized (this) {
                if (loc != fallbackLocale) {
                    fallbackLocale = loc;
                    fallbackLocaleName = loc.getBaseName();
                    clearServiceCache();
                }
            }
        }
        return fallbackLocaleName;
    }

    public Key createKey(String id) {
        return LocaleKey.createWithCanonicalFallback(id, validateFallbackLocale());
    }

    public Key createKey(String id, int kind) {
        return LocaleKey.createWithCanonicalFallback(id, validateFallbackLocale(), kind);
    }

    public Key createKey(ULocale l, int kind) {
        return LocaleKey.createWithCanonical(l, validateFallbackLocale(), kind);
    }
}
