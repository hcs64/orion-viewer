/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
 *
 * Copyright (C) 2011-2013  Michael Bogdanov & Co
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package universe.constellation.orion.viewer.selection;

import android.graphics.Point;
import android.os.SystemClock;
import android.view.MotionEvent;
import universe.constellation.orion.viewer.Common;
import universe.constellation.orion.viewer.OrionView;
import universe.constellation.orion.viewer.OrionViewerActivity;
import universe.constellation.orion.viewer.android.touch.AndroidScaleWrapper;
import universe.constellation.orion.viewer.android.touch.OldAdroidScaleWrapper;
import universe.constellation.orion.viewer.android.touch.ScaleDetectorWrapper;

/**
* User: mike
* Date: 02.01.13
* Time: 18:57
*/
public class TouchAutomata extends TouchAutomataOldAndroid {

    private Point startFocus = new Point();

    private Point endFocus = new Point();

    private float curScale = 1f;

    private ScaleDetectorWrapper gestureDetector;

    public TouchAutomata(OrionViewerActivity activity, OrionView view) {
        super(activity, view);
        int sdkVersion = activity.getOrionContext().getSdkVersion();
        gestureDetector = sdkVersion >= 8 ? new AndroidScaleWrapper(activity, this) :
                sdkVersion >= 5 ?  new OldAdroidScaleWrapper(activity, this) : null;
    }

    public void startAutomata() {
        reset();
    }

    public boolean onTouch(MotionEvent event) {
        return onTouch(event, null, 0);
    }

    public boolean onTouch(MotionEvent event, PinchEvents pinch, float scale) {
        boolean processed = false;

        if (pinch == null && gestureDetector != null) {
            gestureDetector.onTouchEvent(event);
            if (gestureDetector.isInProgress()) {
                return true;
            }
        }

        switch (currentState) {
            case UNDEFINED:
                if (PinchEvents.START_SCALE == pinch) {
                    nextState = States.PINCH_ZOOM;
                    processed = true;
                } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startTime = SystemClock.uptimeMillis();
                    start0.x = (int) event.getX();
                    start0.y = (int) event.getY();
                    last0.x = start0.x;
                    last0.y = start0.y;
                    nextState = States.SINGLE_CLICK;
                    processed = true;
                }
                break;

            case SINGLE_CLICK:
                if (PinchEvents.START_SCALE == pinch) {
                    nextState = States.PINCH_ZOOM;
                    processed = true;
                } else {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        last0.x = start0.x;
                        last0.y = start0.y;
                        processed = true;
                        System.out.println("In action down twice");
                    }

                    if (event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_UP) {
                        boolean doAction = false;
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            Common.d("UP " + event.getAction());
                            doAction = true;
                        } else {
                            if (last0.x != -1 && last0.y != -1) {
                                boolean isLongClick = (SystemClock.uptimeMillis() - startTime) > TIME_DELTA;
                                doAction = isLongClick;
                            }
                        }

                        if (doAction) {
                            Common.d("Check event action " + event.getAction());
                            boolean isLongClick = (SystemClock.uptimeMillis() - startTime) > TIME_DELTA;

                            if (last0.x != -1 && last0.y != -1) {
                                int width = getView().getWidth();
                                int height = getView().getHeight();

                                int i = 3 * last0.y / height;
                                int j = 3 * last0.x / width;

                                int code = activity.getGlobalOptions().getActionCode(i, j, isLongClick);
                                activity.doAction(code);

                                nextState = States.UNDEFINED;
                            }

                        }
                    }

                }
                break;

            case PINCH_ZOOM:
  //              System.out.println("pinch " + pinch);
                if (pinch != null) {
                    switch (pinch) {
                        case START_SCALE:
                            curScale = gestureDetector.getScaleFactor();
                            startFocus.x = (int) gestureDetector.getFocusX();
                            startFocus.y = (int) gestureDetector.getFocusY();
                            break;
                        case DO_SCALE:
                            curScale *= gestureDetector.getScaleFactor();
                            endFocus.x = (int) gestureDetector.getFocusX();
                            endFocus.y = (int) gestureDetector.getFocusY();
                            getView().doScale(curScale, startFocus, endFocus);
                            getView().invalidate();
                            //System.out.println(endFocus.x + " onscale " + endFocus.y);
                            break;
                        case END_SCALE:
                            getView().doScale(1f, null, null);
                            nextState = States.UNDEFINED;
                            //System.out.println(endFocus.x + " xxxx " + endFocus.y);
                            float newX = (int) ((startFocus.x) * (curScale - 1) + (startFocus.x - endFocus.x) );
                            float newY = (int) ((startFocus.y) * (curScale - 1) + (startFocus.y - endFocus.y));
                            activity.getController().translateAndZoom(curScale, newX, newY);
                            break;
                    }
                } else {
                    nextState = States.UNDEFINED;
                }
                processed = true;
                break;
        }

        if (nextState != currentState) {
            System.out.println("Next state = " + nextState);
            switch (nextState) {
                case UNDEFINED: reset(); break;
                case PINCH_ZOOM:
                    curScale = gestureDetector.getScaleFactor();
                    startFocus.x = (int) gestureDetector.getFocusX();
                    startFocus.y = (int) gestureDetector.getFocusY();
                    endFocus.x = startFocus.x;
                    endFocus.y = startFocus.y;
                    break;
            }
        }
        currentState = nextState;
        return processed;
    }
}
