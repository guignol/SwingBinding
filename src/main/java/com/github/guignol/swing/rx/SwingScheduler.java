package com.github.guignol.swing.rx;

import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;

import javax.swing.*;
import java.util.concurrent.TimeUnit;

/**
 * Swing Scheduler for RxJava 1.x
 * https://github.com/ReactiveX/RxSwing/blob/0.x/src/main/java/rx/schedulers/SwingScheduler.java
 * <p>
 * Swing Scheduler for RxJava 2.x (not yet merged on RxSwing)
 * https://github.com/jutoft/RxSwing/blob/4b3c2a93dca934c7d857151b4ca1554e9853445e/src/main/java/io/reactivex/schedulers/SwingScheduler.java
 * <p>
 * Swing Scheduler for RxJava 2.x (not yet merged on RxSwing2)
 * https://github.com/UeliKurmann/RxSwing2/blob/8012ae881aa58cbb829433554489f9b83e6411ea/src/main/java/rx/schedulers/SwingScheduler.java
 * <p>
 * Android Scheduler for RxJava 1.x
 * https://github.com/ReactiveX/RxAndroid/blob/1.x/rxandroid/src/main/java/rx/android/schedulers/LooperScheduler.java
 * <p>
 * Android Scheduler for RxJava 2.x
 * https://github.com/ReactiveX/RxAndroid/blob/2.x/rxandroid/src/main/java/io/reactivex/android/schedulers/HandlerScheduler.java
 */
public class SwingScheduler extends Scheduler {

    private static final SwingScheduler INSTANCE = new SwingScheduler();

    public static SwingScheduler getInstance() {
        return INSTANCE;
    }

    private SwingScheduler() {
    }

    @Override
    public Disposable scheduleDirect(Runnable run) {
        // TODO RxAndroidは何故これをオーバーライドしてるのか
        // TODO このまま任せると、DisposableTaskでdisposeされる可能性がある？
        return super.scheduleDirect(run);
    }

    @Override
    public Worker createWorker() {
        return new InnerSwingScheduler();
    }

    private static class InnerSwingScheduler extends Worker {

        private final CompositeDisposable composite = new CompositeDisposable();

        @Override
        public Disposable schedule(Runnable original, long delayTime, TimeUnit unit) {
            if (original == null) throw new NullPointerException("run == null");
            if (unit == null) throw new NullPointerException("unit == null");

            final long delay = Math.max(0, unit.toMillis(delayTime));
            assertThatTheDelayIsValidForTheSwingTimer(delay);

            final Disposable local;
            if (delay == 0) {
                local = Disposables.empty();
                if (SwingUtilities.isEventDispatchThread()) {
                    // 即時実行
                    original.run();
                } else {
                    SwingUtilities.invokeLater(() -> {
                        if (composite.isDisposed() || local.isDisposed()) {
                            return;
                        }
                        original.run();
                        composite.remove(local);
                    });
                }
            } else {
                final Timer timer = new Timer((int) delay, null);
                local = Disposables.fromRunnable(timer::stop);
                timer.setRepeats(false);
                timer.addActionListener(e -> {
                    if (composite.isDisposed() || local.isDisposed()) {
                        return;
                    }
                    original.run();
                    composite.remove(local);
                });
                timer.start();
            }
            composite.add(local);

            // UeliKurmannのSwingSchedulerはWorkerのCompositeDisposableを返していて、タスク単位のdisposeができない
            // AndroidのSchedulerもタスク単位のdisposeを提供しているので必要だと思うが、違いの出る利用パターンを見つけられていない
            return local;
        }

        @Override
        public void dispose() {
            composite.dispose();
        }

        @Override
        public boolean isDisposed() {
            return composite.isDisposed();
        }

        private static void assertThatTheDelayIsValidForTheSwingTimer(long delay) {
            if (delay < 0 || delay > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(String.format("The swing timer only accepts non-negative delays up to %d milliseconds.", Integer.MAX_VALUE));
            }
        }
    }
}
