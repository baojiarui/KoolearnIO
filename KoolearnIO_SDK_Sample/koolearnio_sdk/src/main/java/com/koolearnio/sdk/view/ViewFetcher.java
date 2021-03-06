package com.koolearnio.sdk.view;

import android.app.Application;
import android.content.Context;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;

import com.koolearnio.sdk.KDUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Contains view methods. Examples are getViews(),
 * getCurrentTextViews(), getCurrentImageViews().
 *
 * @author Renas Reda, renas.reda@robotium.com
 *
 */

public class ViewFetcher {

    private String windowManagerString;
    private Application application;


    public ViewFetcher(Application application) {
        this.application = application;
        setWindowManagerString();
    }



    /**
     * Returns the scroll or list parent view
     *
     * @param view the view who's parent should be returned
     * @return the parent scroll view, list view or null
     */

    public View getScrollOrListParent(View view) {

        if (!(view instanceof android.widget.AbsListView) && !(view instanceof android.widget.ScrollView) && !(view instanceof WebView)) {
            try{
                return getScrollOrListParent((View) view.getParent());
            }catch(Exception e){
                return null;
            }
        } else {
            return view;
        }
    }

    /**
     * Returns views from the shown DecorViews.
     *
     * @param onlySufficientlyVisible if only sufficiently visible views should be returned
     * @return all the views contained in the DecorViews
     */

    public ArrayList<View> getAllViews(boolean onlySufficientlyVisible) {
        final View[] views = getWindowDecorViews();
        final ArrayList<View> allViews = new ArrayList<View>();
        final View[] nonDecorViews = getNonDecorViews(views);
        View view = null;

        if(nonDecorViews != null){
            for(int i = 0; i < nonDecorViews.length; i++){
                view = nonDecorViews[i];
                try {
                    addChildren(allViews, (ViewGroup)view, onlySufficientlyVisible);
                } catch (Exception ignored) {}
                if(view != null) allViews.add(view);
            }
        }

        if (views != null && views.length > 0) {
            view = getRecentDecorView(views);
            try {
                addChildren(allViews, (ViewGroup)view, onlySufficientlyVisible);
            } catch (Exception ignored) {}

            if(view != null) allViews.add(view);
        }

        return allViews;
    }


    /**
     * 获取带ViewPath的页面所有View
     * @param onlySufficientlyVisible
     * @return
     */
    public ArrayList<KDView> getAllViewsWithId(boolean onlySufficientlyVisible){
        final ArrayList<KDView> allKDViews = new ArrayList<>();
        final View[] views = getWindowDecorViews();
        final View[] nonDecorViews = getNonDecorViews(views);
        View view = null;

        if(nonDecorViews != null){
            for(int i = 0; i < nonDecorViews.length; i++){
                view = nonDecorViews[i];
                try {
                    addKDViewChildren(allKDViews, new KDViewGroup((ViewGroup)view,view.getClass().getSimpleName()), onlySufficientlyVisible);
                } catch (Exception ignored) {

                }
                if(view != null)
                    allKDViews.add(
                            new KDView(view.getClass().getSimpleName(),
                                    view.getId(),view));
            }
        }

        if (views != null && views.length > 0) {
            view = getRecentDecorView(views);
            try {
                addKDViewChildren(allKDViews, new KDViewGroup((ViewGroup)view,view.getClass().getSimpleName()), onlySufficientlyVisible);
            } catch (Exception ignored) {}

            if(view != null)
                allKDViews.add(
                    new KDView(view.getClass().getSimpleName(),
                            view.getId(),view));
        }

        return allKDViews;
    }


    private void addKDViewChildren(ArrayList<KDView> views, KDViewGroup viewGroup, boolean onlySufficientlyVisible) {
        if(viewGroup != null){
            for (int i = 0; i < viewGroup.getmViewGroup().getChildCount(); i++) {
                final View child = viewGroup.getmViewGroup().getChildAt(i);
                String childViewPath = viewGroup.getViewPath()+"/"+child.getClass().getSimpleName()+"["+i+"]";
                if(onlySufficientlyVisible && isViewSufficientlyShown(child)) {
                    views.add(new KDView(childViewPath,child.getId(),child));
                }

                else if(!onlySufficientlyVisible && child != null) {
                    views.add(new KDView(childViewPath,child.getId(),child));
                }

                if (child instanceof ViewGroup) {
                    addKDViewChildren(views, new KDViewGroup((ViewGroup)child,childViewPath), onlySufficientlyVisible);
                }
            }
        }
    }

    /**
     * Returns the most recent DecorView
     *
     * @param views the views to check
     * @return the most recent DecorView
     */

    public final View getRecentDecorView(View[] views) {
        if(views == null)
            return null;

        final View[] decorViews = new View[views.length];
        int i = 0;
        View view;

        for (int j = 0; j < views.length; j++) {
            view = views[j];
            if (isDecorView(view)){
                decorViews[i] = view;
                i++;
            }
        }
        return getRecentContainer(decorViews);
    }

    /**
     * Returns the most recent view container
     *
     * @param views the views to check
     * @return the most recent view container
     */

    private final View getRecentContainer(View[] views) {
        View container = null;
        long drawingTime = 0;
        View view;

        for(int i = 0; i < views.length; i++){
            view = views[i];
            if (view != null && view.isShown() && view.hasWindowFocus() && view.getDrawingTime() > drawingTime) {
                container = view;
                drawingTime = view.getDrawingTime();
            }
        }
        return container;
    }

    /**
     * Returns all views that are non DecorViews
     *
     * @param views the views to check
     * @return the non DecorViews
     */

    private final View[] getNonDecorViews(View[] views) {
        View[] decorViews = null;

        if(views != null) {
            decorViews = new View[views.length];

            int i = 0;
            View view;

            for (int j = 0; j < views.length; j++) {
                view = views[j];
                if (!isDecorView(view)) {
                    decorViews[i] = view;
                    i++;
                }
            }
        }
        return decorViews;
    }

    /**
     * Returns whether a view is a DecorView
     * @param view
     * @return true if view is a DecorView, false otherwise
     */
    private boolean isDecorView(View view) {
        if (view == null) {
            return false;
        }

        final String nameOfClass = view.getClass().getName();
        return (nameOfClass.equals("com.android.internal.policy.impl.PhoneWindow$DecorView") ||
                nameOfClass.equals("com.android.internal.policy.impl.MultiPhoneWindow$MultiPhoneDecorView") ||
                nameOfClass.equals("com.android.internal.policy.PhoneWindow$DecorView"));
    }


    /**
     * Extracts all {@code View}s located in the currently active {@code Activity}, recursively.
     *
     * @param parent the {@code View} whose children should be returned, or {@code null} for all
     * @param onlySufficientlyVisible if only sufficiently visible views should be returned
     * @return all {@code View}s located in the currently active {@code Activity}, never {@code null}
     */

    public ArrayList<View> getViews(View parent, boolean onlySufficientlyVisible) {
        final ArrayList<View> views = new ArrayList<View>();
        final View parentToUse;

        if (parent == null){
            return getAllViews(onlySufficientlyVisible);
        }else{
            parentToUse = parent;

            views.add(parentToUse);

            if (parentToUse instanceof ViewGroup) {
                addChildren(views, (ViewGroup) parentToUse, onlySufficientlyVisible);
            }
        }
        return views;
    }

    /**
     * Adds all children of {@code viewGroup} (recursively) into {@code views}.
     *
     * @param views an {@code ArrayList} of {@code View}s
     * @param viewGroup the {@code ViewGroup} to extract children from
     * @param onlySufficientlyVisible if only sufficiently visible views should be returned
     */

    private void addChildren(ArrayList<View> views, ViewGroup viewGroup, boolean onlySufficientlyVisible) {
        if(viewGroup != null){
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                final View child = viewGroup.getChildAt(i);

                if(onlySufficientlyVisible && isViewSufficientlyShown(child)) {
                    views.add(child);
                }

                else if(!onlySufficientlyVisible && child != null) {
                    views.add(child);
                }

                if (child instanceof ViewGroup) {
                    addChildren(views, (ViewGroup) child, onlySufficientlyVisible);
                }
            }
        }
    }

    /**
     * Returns true if the view is sufficiently shown
     *
     * @param view the view to check
     * @return true if the view is sufficiently shown
     */

    public final boolean isViewSufficientlyShown(View view){
        final int[] xyView = new int[2];
        final int[] xyParent = new int[2];

        if(view == null)
            return false;

        final float viewHeight = view.getHeight();
        final View parent = getScrollOrListParent(view);
        view.getLocationOnScreen(xyView);

        if(parent == null){
            xyParent[1] = 0;
        }
        else{
            parent.getLocationOnScreen(xyParent);
        }

        if(xyView[1] + (viewHeight/2.0f) > getScrollListWindowHeight(view))
            return false;

        else if(xyView[1] + (viewHeight/2.0f) < xyParent[1])
            return false;

        return true;
    }

    /**
     * Returns the height of the scroll or list view parent
     * @param view the view who's parents height should be returned
     * @return the height of the scroll or list view parent
     */

    @SuppressWarnings("deprecation")
    public float getScrollListWindowHeight(View view) {
        final int[] xyParent = new int[2];
        View parent = getScrollOrListParent(view);
        final float windowHeight;

        if(parent == null){
            WindowManager windowManager = (WindowManager)
                    application.getSystemService(Context.WINDOW_SERVICE);

            windowHeight = windowManager.getDefaultDisplay().getHeight();
        }

        else{
            parent.getLocationOnScreen(xyParent);
            windowHeight = xyParent[1] + parent.getHeight();
        }
        parent = null;
        return windowHeight;
    }


    /**
     * Returns an {@code ArrayList} of {@code View}s of the specified {@code Class} located in the current
     * {@code Activity}.
     *
     * @param classToFilterBy return all instances of this class, e.g. {@code Button.class} or {@code GridView.class}
     * @param includeSubclasses include instances of the subclasses in the {@code ArrayList} that will be returned
     * @return an {@code ArrayList} of {@code View}s of the specified {@code Class} located in the current {@code Activity}
     */

    public <T extends View> ArrayList<T> getCurrentViews(Class<T> classToFilterBy, boolean includeSubclasses) {
        return getCurrentViews(classToFilterBy, includeSubclasses, null);
    }

    /**
     * Returns an {@code ArrayList} of {@code View}s of the specified {@code Class} located under the specified {@code parent}.
     *
     * @param classToFilterBy return all instances of this class, e.g. {@code Button.class} or {@code GridView.class}
     * @param includeSubclasses include instances of subclasses in {@code ArrayList} that will be returned
     * @param parent the parent {@code View} for where to start the traversal
     * @return an {@code ArrayList} of {@code View}s of the specified {@code Class} located under the specified {@code parent}
     */

    public <T extends View> ArrayList<T> getCurrentViews(Class<T> classToFilterBy, boolean includeSubclasses, View parent) {
        ArrayList<T> filteredViews = new ArrayList<T>();
        List<View> allViews = getViews(parent, true);
        for(View view : allViews){
            if (view == null) {
                continue;
            }
            Class<? extends View> classOfView = view.getClass();
            if (includeSubclasses && classToFilterBy.isAssignableFrom(classOfView) || !includeSubclasses && classToFilterBy == classOfView) {
                filteredViews.add(classToFilterBy.cast(view));
            }
        }
        allViews = null;
        return filteredViews;
    }


    /**
     * Tries to guess which view is the most likely to be interesting. Returns
     * the most recently drawn view, which presumably will be the one that the
     * user was most recently interacting with.
     *
     * @return most recently drawn view, or null if no views were passed
     */

    public final <T extends View> T getFreshestView(ArrayList<T> views){
        final int[] locationOnScreen = new int[2];
        T viewToReturn = null;
        long drawingTime = 0;
        if(views == null){
            return null;
        }
        for(T view : views){
            if(view != null){
                view.getLocationOnScreen(locationOnScreen);

                if (locationOnScreen[0] < 0 || !(view.getHeight() > 0)){
                    continue;
                }

                if(view.getDrawingTime() > drawingTime){
                    drawingTime = view.getDrawingTime();
                    viewToReturn = view;
                }
                else if (view.getDrawingTime() == drawingTime){
                    if(view.isFocused()){
                        viewToReturn = view;
                    }
                }

            }
        }
        views = null;
        return viewToReturn;
    }


    /**
     * Waits for a RecyclerView and returns it.
     *
     * @param recyclerViewIndex the index of the RecyclerView
     * @return {@code ViewGroup} if RecycleView is displayed
     */


    public <T extends View> ViewGroup getRecyclerView(int recyclerViewIndex, int timeOut) {
        final long endTime = SystemClock.uptimeMillis() + timeOut;

        while (SystemClock.uptimeMillis() < endTime) {
            View recyclerView = getRecyclerView(true, recyclerViewIndex);
            if(recyclerView != null){
                return (ViewGroup) recyclerView;
            }
        }
        return null;
    }


    /**
     * Returns a RecyclerView or null if none is found
     *
     *
     * @return a RecyclerView
     */

    public View getRecyclerView(boolean shouldSleep, int recyclerViewIndex){
        Set<View> uniqueViews = new HashSet<View>();

        @SuppressWarnings("unchecked")
        ArrayList<View> views = KDUtils.filterViewsToSet(new Class[] {ViewGroup.class}, getAllViews(false));
        views = KDUtils.removeInvisibleViews(views);

        for(View view : views){

            if(isViewType(view.getClass(), "widget.RecyclerView")){
                uniqueViews.add(view);
            }

            if(uniqueViews.size() > recyclerViewIndex) {
                return (ViewGroup) view;
            }
        }
        return null;
    }

    /**
     * Returns a Set of all RecyclerView or empty Set if none is found
     *
     *
     * @return a Set of RecyclerViews
     */

    public List<View> getScrollableSupportPackageViews(boolean shouldSleep){
        List<View> viewsToReturn = new ArrayList<View>();
//        if(shouldSleep){
//            sleeper.sleep();
//        }

        @SuppressWarnings("unchecked")
        ArrayList<View> views = KDUtils.filterViewsToSet(new Class[] {ViewGroup.class}, getAllViews(true));
        views = KDUtils.removeInvisibleViews(views);

        for(View view : views){

            if(isViewType(view.getClass(), "widget.RecyclerView") ||
                    isViewType(view.getClass(), "widget.NestedScrollView")){
                viewsToReturn.add(view);
            }

        }
        return viewsToReturn;
    }

    private boolean isViewType(Class<?> aClass, String typeName) {
        if (aClass.getName().contains(typeName)) {
            return true;
        }

        if (aClass.getSuperclass() != null) {
            return isViewType(aClass.getSuperclass(), typeName);
        }

        return false;
    }

    /**
     * Returns an identical View to the one specified.
     *
     * @param view the view to find
     * @return identical view of the specified view
     */

    public View getIdenticalView(View view) {
        if(view == null){
            return null;
        }
        View viewToReturn = null;
        List<? extends View> visibleViews = KDUtils.removeInvisibleViews(getCurrentViews(view.getClass(), true));

        for(View v : visibleViews){
            if(areViewsIdentical(v, view)){
                viewToReturn = v;
                break;
            }
        }

        return viewToReturn;
    }

    /**
     * Compares if the specified views are identical. This is used instead of View.compare
     * as it always returns false in cases where the View tree is refreshed.
     *
     * @param firstView the first view
     * @param secondView the second view
     * @return true if views are equal
     */

    private boolean areViewsIdentical(View firstView, View secondView){
        if(firstView.getId() != secondView.getId() || !firstView.getClass().isAssignableFrom(secondView.getClass())){
            return false;
        }

        if (firstView.getParent() != null && firstView.getParent() instanceof View &&
                secondView.getParent() != null && secondView.getParent() instanceof View) {

            return areViewsIdentical((View) firstView.getParent(), (View) secondView.getParent());
        } else {
            return true;
        }
    }

    private static Class<?> windowManager;
    static{
        try {
            String windowManagerClassName;
            if (android.os.Build.VERSION.SDK_INT >= 17) {
                windowManagerClassName = "android.view.WindowManagerGlobal";
            } else {
                windowManagerClassName = "android.view.WindowManagerImpl";
            }
            windowManager = Class.forName(windowManagerClassName);

        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the WindorDecorViews shown on the screen.
     *
     * @return the WindorDecorViews shown on the screen
     */

    @SuppressWarnings("unchecked")
    public View[] getWindowDecorViews()
    {

        Field viewsField;
        Field instanceField;
        try {
            viewsField = windowManager.getDeclaredField("mViews");
            instanceField = windowManager.getDeclaredField(windowManagerString);
            viewsField.setAccessible(true);
            instanceField.setAccessible(true);
            Object instance = instanceField.get(null);
            View[] result;
            if (android.os.Build.VERSION.SDK_INT >= 19) {
                result = ((ArrayList<View>) viewsField.get(instance)).toArray(new View[0]);
            } else {
                result = (View[]) viewsField.get(instance);
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Sets the window manager string.
     */
    private void setWindowManagerString(){

        if (android.os.Build.VERSION.SDK_INT >= 17) {
            windowManagerString = "sDefaultWindowManager";

        } else if(android.os.Build.VERSION.SDK_INT >= 13) {
            windowManagerString = "sWindowManager";

        } else {
            windowManagerString = "mWindowManager";
        }
    }


}
