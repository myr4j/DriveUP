package com.driveup.ui.stat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.driveup.R;
import com.driveup.databinding.FragmentDashboardBinding;
import com.driveup.databinding.ItemStatBarBinding;
import com.driveup.databinding.ItemStatPeriodBinding;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatFragment extends Fragment {

    private static final Locale FRENCH_LOCALE = Locale.FRANCE;
    
    private static final String[] MONTH_NAMES = {
        "", "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
        "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
    };
    
    private FragmentDashboardBinding binding;
    private StatViewModel statViewModel;
    private NumberFormat currencyFormat;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        statViewModel = new ViewModelProvider(
                requireActivity(),
                new androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory(
                        requireActivity().getApplication()))
                .get(StatViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        currencyFormat = NumberFormat.getCurrencyInstance(FRENCH_LOCALE);
        
        observeViewModel();
        
        return binding.getRoot();
    }

    private void observeViewModel() {
        statViewModel.getTotalStats().observe(getViewLifecycleOwner(), totalStats -> {
            if (totalStats != null) {
                updateTotalStats(totalStats);
            }
        });

        statViewModel.getDayOfWeekStats().observe(getViewLifecycleOwner(), dayStats -> {
            if (dayStats != null) {
                updateDayOfWeekStats(dayStats);
            }
        });

        statViewModel.getTimeSlotStats().observe(getViewLifecycleOwner(), timeSlotStats -> {
            if (timeSlotStats != null) {
                updateTimeSlotStats(timeSlotStats);
            }
        });

        statViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (!isLoading && statViewModel.getTotalStats().getValue() == null) {
                showEmptyState();
            } else {
                hideEmptyState();
            }
        });
    }

    private void showEmptyState() {
        binding.textEmptyStats.setVisibility(View.VISIBLE);
        setKPICardsVisibility(View.GONE);
        binding.cardDayStats.setVisibility(View.GONE);
        binding.cardTimeSlotStats.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        binding.textEmptyStats.setVisibility(View.GONE);
        setKPICardsVisibility(View.VISIBLE);
        binding.cardDayStats.setVisibility(View.VISIBLE);
        binding.cardTimeSlotStats.setVisibility(View.VISIBLE);
    }

    private void setKPICardsVisibility(int visibility) {
        View root = binding.getRoot();
        if (root != null) {
            View cardRides = root.findViewById(R.id.card_total_rides);
            View cardAmount = root.findViewById(R.id.card_total_amount);
            if (cardRides != null) cardRides.setVisibility(visibility);
            if (cardAmount != null) cardAmount.setVisibility(visibility);
        }
    }

    private void updateTotalStats(TotalStats totalStats) {
        binding.textTotalRides.setText(String.valueOf(totalStats.getTotalRides()));
        binding.textTotalAmount.setText(currencyFormat.format(totalStats.getTotalAmount()));
    }

    private void updateDayOfWeekStats(DayOfWeekStats dayStats) {
        LinearLayout monthLayout = binding.layoutMonthDays;
        LinearLayout yearLayout = binding.layoutYearDays;
        
        clearLayout(monthLayout);
        clearLayout(yearLayout);

        updatePeriodStats(
                dayStats.getStatsByMonth(),
                monthLayout,
                this::createDayStatsView,
                this::createSingleDayStatsView,
                "Aucune donnée pour les mois"
        );

        updatePeriodStats(
                convertYearStatsToGenericMap(dayStats.getStatsByYear()),
                yearLayout,
                this::createDayStatsView,
                this::createSingleDayStatsView,
                "Aucune donnée pour les années"
        );
    }

    private void updateTimeSlotStats(TimeSlotStats timeSlotStats) {
        LinearLayout monthLayout = binding.layoutMonthTimeslots;
        LinearLayout yearLayout = binding.layoutYearTimeslots;
        
        clearLayout(monthLayout);
        clearLayout(yearLayout);

        updatePeriodStats(
                timeSlotStats.getStatsByMonth(),
                monthLayout,
                this::createTimeSlotStatsView,
                this::createSingleTimeSlotStatsView,
                "Aucune donnée pour les mois"
        );

        updatePeriodStats(
                convertYearTimeSlotStatsToGenericMap(timeSlotStats.getStatsByYear()),
                yearLayout,
                this::createTimeSlotStatsView,
                this::createSingleTimeSlotStatsView,
                "Aucune donnée pour les années"
        );
    }

    @FunctionalInterface
    private interface StatViewCreator<T> {
        View create(String period, T most, T least);
    }

    @FunctionalInterface
    private interface SingleStatViewCreator<T> {
        View create(String period, T stat);
    }

    private <T> void updatePeriodStats(
            Map<String, List<T>> statsByPeriod,
            LinearLayout layout,
            StatViewCreator<T> viewCreator,
            SingleStatViewCreator<T> singleViewCreator,
            String emptyMessage) {
        
        if (statsByPeriod == null || statsByPeriod.isEmpty()) {
            layout.addView(createEmptyTextView(emptyMessage));
            return;
        }

        List<Map.Entry<String, List<T>>> sortedEntries = new ArrayList<>(statsByPeriod.entrySet());
        sortedEntries.sort(Map.Entry.comparingByKey());

        for (Map.Entry<String, List<T>> entry : sortedEntries) {
            String periodKey = entry.getKey();
            List<T> stats = entry.getValue();

            if (stats.isEmpty()) {
                continue;
            }

            String periodName = getPeriodName(periodKey);
            View periodView;

            if (stats.size() == 1) {
                periodView = singleViewCreator.create(periodName, stats.get(0));
            } else {
                T mostProfitable = stats.get(0);
                T leastProfitable = stats.get(stats.size() - 1);
                periodView = viewCreator.create(periodName, mostProfitable, leastProfitable);
            }

            layout.addView(periodView);
        }
    }

    private View createSingleDayStatsView(String period, DayOfWeekStats.DayStats stats) {
        ItemStatPeriodBinding periodBinding = createPeriodBinding();
        periodBinding.textPeriod.setText(period);

        String label = stats.getRideCount() == 1 
                ? "Une seule course: " + stats.getDayOfWeek()
                : "Un seul jour: " + stats.getDayOfWeek();
        
        View barView = createStatBarView(label, stats.getTotalAmount(), stats.getRideCount());
        periodBinding.layoutStats.addView(barView);

        return periodBinding.getRoot();
    }

    private View createDayStatsView(String period, DayOfWeekStats.DayStats most, 
                                    DayOfWeekStats.DayStats least) {
        return createStatsView(
                period,
                "Plus rentable: " + most.getDayOfWeek(),
                most.getTotalAmount(),
                most.getRideCount(),
                "Moins rentable: " + least.getDayOfWeek(),
                least.getTotalAmount(),
                least.getRideCount()
        );
    }

    private View createSingleTimeSlotStatsView(String period, TimeSlotStats.TimeSlotData stats) {
        ItemStatPeriodBinding periodBinding = createPeriodBinding();
        periodBinding.textPeriod.setText(period);

        String label = stats.getRideCount() == 1
                ? "Une seule course: " + stats.getTimeSlot()
                : "Un seul jour: " + stats.getTimeSlot();
        
        View barView = createStatBarView(label, stats.getTotalAmount(), stats.getRideCount());
        periodBinding.layoutStats.addView(barView);

        return periodBinding.getRoot();
    }

    private View createTimeSlotStatsView(String period, TimeSlotStats.TimeSlotData most,
                                         TimeSlotStats.TimeSlotData least) {
        return createStatsView(
                period,
                "Plus rentable: " + most.getTimeSlot(),
                most.getTotalAmount(),
                most.getRideCount(),
                "Moins rentable: " + least.getTimeSlot(),
                least.getTotalAmount(),
                least.getRideCount()
        );
    }

    private View createStatsView(String period, String mostLabel, double mostAmount, int mostCount,
                                 String leastLabel, double leastAmount, int leastCount) {
        ItemStatPeriodBinding periodBinding = createPeriodBinding();
        periodBinding.textPeriod.setText(period);

        View mostView = createStatBarView(mostLabel, mostAmount, mostCount);
        periodBinding.layoutStats.addView(mostView);

        View leastView = createStatBarView(leastLabel, leastAmount, leastCount);
        periodBinding.layoutStats.addView(leastView);

        return periodBinding.getRoot();
    }

    private View createStatBarView(String label, double amount, int count) {
        ItemStatBarBinding barBinding = ItemStatBarBinding.inflate(
                LayoutInflater.from(requireContext()));

        barBinding.textLabel.setText(label);
        barBinding.textValue.setText(currencyFormat.format(amount));
        barBinding.textCount.setText(formatRideCount(count));

        return barBinding.getRoot();
    }

    private String formatRideCount(int count) {
        return "(" + count + " course" + (count > 1 ? "s" : "") + ")";
    }

    private ItemStatPeriodBinding createPeriodBinding() {
        return ItemStatPeriodBinding.inflate(LayoutInflater.from(requireContext()));
    }

    private TextView createEmptyTextView(String message) {
        TextView emptyText = new TextView(requireContext());
        emptyText.setText(message);
        emptyText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
        return emptyText;
    }

    private void clearLayout(LinearLayout layout) {
        layout.removeAllViews();
    }

    private String getPeriodName(String periodKey) {
        if (periodKey.contains("-")) {
            return getMonthName(periodKey);
        }
        return periodKey;
    }

    private String getMonthName(String monthKey) {
        String[] parts = monthKey.split("-");
        if (parts.length != 2) {
            return monthKey;
        }

        try {
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);

            if (month >= 1 && month <= 12) {
                return MONTH_NAMES[month] + " " + year;
            }
        } catch (NumberFormatException e) {
            // Log error if needed
        }

        return monthKey;
    }

    private Map<String, List<DayOfWeekStats.DayStats>> convertYearStatsToGenericMap(
            Map<Integer, List<DayOfWeekStats.DayStats>> statsByYear) {
        Map<String, List<DayOfWeekStats.DayStats>> result = new java.util.LinkedHashMap<>();
        for (Map.Entry<Integer, List<DayOfWeekStats.DayStats>> entry : statsByYear.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private Map<String, List<TimeSlotStats.TimeSlotData>> convertYearTimeSlotStatsToGenericMap(
            Map<Integer, List<TimeSlotStats.TimeSlotData>> statsByYear) {
        Map<String, List<TimeSlotStats.TimeSlotData>> result = new java.util.LinkedHashMap<>();
        for (Map.Entry<Integer, List<TimeSlotStats.TimeSlotData>> entry : statsByYear.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (statViewModel != null) {
            statViewModel.refreshStatistics();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
