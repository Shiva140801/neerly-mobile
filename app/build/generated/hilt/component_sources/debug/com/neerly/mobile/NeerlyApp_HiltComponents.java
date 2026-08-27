package com.neerly.mobile;

import com.neerly.mobile.data.api.NetworkModule;
import com.neerly.mobile.feature.address.AddressFormViewModel_HiltModules;
import com.neerly.mobile.feature.address.AddressListViewModel_HiltModules;
import com.neerly.mobile.feature.auth.AuthViewModel_HiltModules;
import com.neerly.mobile.feature.cart.CartViewModel_HiltModules;
import com.neerly.mobile.feature.checkout.CheckoutViewModel_HiltModules;
import com.neerly.mobile.feature.complaint.ComplaintFileViewModel_HiltModules;
import com.neerly.mobile.feature.complaint.ComplaintThreadViewModel_HiltModules;
import com.neerly.mobile.feature.customer.CustomerHomeViewModel_HiltModules;
import com.neerly.mobile.feature.customer.VendorDetailViewModel_HiltModules;
import com.neerly.mobile.feature.deposit.DepositsViewModel_HiltModules;
import com.neerly.mobile.feature.driver.DriverCodReconcileViewModel_HiltModules;
import com.neerly.mobile.feature.driver.DriverHomeViewModel_HiltModules;
import com.neerly.mobile.feature.event.EventBookingViewModel_HiltModules;
import com.neerly.mobile.feature.notification.NotificationFeedViewModel_HiltModules;
import com.neerly.mobile.feature.notification.NotificationPrefsViewModel_HiltModules;
import com.neerly.mobile.feature.order.OrderHistoryViewModel_HiltModules;
import com.neerly.mobile.feature.order.OrderTrackingViewModel_HiltModules;
import com.neerly.mobile.feature.profile.ProfileViewModel_HiltModules;
import com.neerly.mobile.feature.review.ReviewSubmitViewModel_HiltModules;
import com.neerly.mobile.feature.subscription.SubscriptionCreateViewModel_HiltModules;
import com.neerly.mobile.feature.subscription.SubscriptionDetailViewModel_HiltModules;
import com.neerly.mobile.feature.subscription.SubscriptionListViewModel_HiltModules;
import com.neerly.mobile.feature.vendor.VendorOnboardingViewModel_HiltModules;
import com.neerly.mobile.feature.vendor.bank.VendorBankViewModel_HiltModules;
import com.neerly.mobile.feature.vendor.catalog.VendorCatalogViewModel_HiltModules;
import com.neerly.mobile.feature.vendor.compliance.VendorComplianceViewModel_HiltModules;
import com.neerly.mobile.feature.vendor.dashboard.VendorTodayViewModel_HiltModules;
import com.neerly.mobile.feature.vendor.earnings.VendorEarningsViewModel_HiltModules;
import com.neerly.mobile.feature.vendor.orders.VendorOrderDetailViewModel_HiltModules;
import com.neerly.mobile.feature.vendor.settings.VendorBusinessConfigViewModel_HiltModules;
import com.neerly.mobile.feature.vendor.settings.VendorSettingsViewModel_HiltModules;
import com.neerly.mobile.feature.vendor.subscriptions.VendorSubscriptionsTodayViewModel_HiltModules;
import com.neerly.mobile.feature.vendor.team.VendorTeamViewModel_HiltModules;
import com.neerly.mobile.feature.wallet.WalletViewModel_HiltModules;
import com.neerly.mobile.push.NeerlyMessagingService_GeneratedInjector;
import dagger.Binds;
import dagger.Component;
import dagger.Module;
import dagger.Subcomponent;
import dagger.hilt.android.components.ActivityComponent;
import dagger.hilt.android.components.ActivityRetainedComponent;
import dagger.hilt.android.components.FragmentComponent;
import dagger.hilt.android.components.ServiceComponent;
import dagger.hilt.android.components.ViewComponent;
import dagger.hilt.android.components.ViewModelComponent;
import dagger.hilt.android.components.ViewWithFragmentComponent;
import dagger.hilt.android.flags.FragmentGetContextFix;
import dagger.hilt.android.flags.HiltWrapper_FragmentGetContextFix_FragmentGetContextFixModule;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.HiltViewModelFactory;
import dagger.hilt.android.internal.lifecycle.HiltWrapper_DefaultViewModelFactories_ActivityModule;
import dagger.hilt.android.internal.lifecycle.HiltWrapper_HiltViewModelFactory_ActivityCreatorEntryPoint;
import dagger.hilt.android.internal.lifecycle.HiltWrapper_HiltViewModelFactory_ViewModelModule;
import dagger.hilt.android.internal.managers.ActivityComponentManager;
import dagger.hilt.android.internal.managers.FragmentComponentManager;
import dagger.hilt.android.internal.managers.HiltWrapper_ActivityRetainedComponentManager_ActivityRetainedComponentBuilderEntryPoint;
import dagger.hilt.android.internal.managers.HiltWrapper_ActivityRetainedComponentManager_ActivityRetainedLifecycleEntryPoint;
import dagger.hilt.android.internal.managers.HiltWrapper_ActivityRetainedComponentManager_LifecycleModule;
import dagger.hilt.android.internal.managers.HiltWrapper_SavedStateHandleModule;
import dagger.hilt.android.internal.managers.ServiceComponentManager;
import dagger.hilt.android.internal.managers.ViewComponentManager;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.HiltWrapper_ActivityModule;
import dagger.hilt.android.scopes.ActivityRetainedScoped;
import dagger.hilt.android.scopes.ActivityScoped;
import dagger.hilt.android.scopes.FragmentScoped;
import dagger.hilt.android.scopes.ServiceScoped;
import dagger.hilt.android.scopes.ViewModelScoped;
import dagger.hilt.android.scopes.ViewScoped;
import dagger.hilt.components.SingletonComponent;
import dagger.hilt.internal.GeneratedComponent;
import dagger.hilt.migration.DisableInstallInCheck;
import javax.annotation.processing.Generated;
import javax.inject.Singleton;

@Generated("dagger.hilt.processor.internal.root.RootProcessor")
public final class NeerlyApp_HiltComponents {
  private NeerlyApp_HiltComponents() {
  }

  @Module(
      subcomponents = ServiceC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface ServiceCBuilderModule {
    @Binds
    ServiceComponentBuilder bind(ServiceC.Builder builder);
  }

  @Module(
      subcomponents = ActivityRetainedC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface ActivityRetainedCBuilderModule {
    @Binds
    ActivityRetainedComponentBuilder bind(ActivityRetainedC.Builder builder);
  }

  @Module(
      subcomponents = ActivityC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface ActivityCBuilderModule {
    @Binds
    ActivityComponentBuilder bind(ActivityC.Builder builder);
  }

  @Module(
      subcomponents = ViewModelC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface ViewModelCBuilderModule {
    @Binds
    ViewModelComponentBuilder bind(ViewModelC.Builder builder);
  }

  @Module(
      subcomponents = ViewC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface ViewCBuilderModule {
    @Binds
    ViewComponentBuilder bind(ViewC.Builder builder);
  }

  @Module(
      subcomponents = FragmentC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface FragmentCBuilderModule {
    @Binds
    FragmentComponentBuilder bind(FragmentC.Builder builder);
  }

  @Module(
      subcomponents = ViewWithFragmentC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface ViewWithFragmentCBuilderModule {
    @Binds
    ViewWithFragmentComponentBuilder bind(ViewWithFragmentC.Builder builder);
  }

  @Component(
      modules = {
          ApplicationContextModule.class,
          HiltWrapper_FragmentGetContextFix_FragmentGetContextFixModule.class,
          ActivityRetainedCBuilderModule.class,
          ServiceCBuilderModule.class,
          NetworkModule.class
      }
  )
  @Singleton
  public abstract static class SingletonC implements NeerlyApp_GeneratedInjector,
      FragmentGetContextFix.FragmentGetContextFixEntryPoint,
      HiltWrapper_ActivityRetainedComponentManager_ActivityRetainedComponentBuilderEntryPoint,
      ServiceComponentManager.ServiceComponentBuilderEntryPoint,
      SingletonComponent,
      GeneratedComponent {
  }

  @Subcomponent
  @ServiceScoped
  public abstract static class ServiceC implements NeerlyMessagingService_GeneratedInjector,
      ServiceComponent,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends ServiceComponentBuilder {
    }
  }

  @Subcomponent(
      modules = {
          AddressFormViewModel_HiltModules.KeyModule.class,
          AddressListViewModel_HiltModules.KeyModule.class,
          AuthViewModel_HiltModules.KeyModule.class,
          CartViewModel_HiltModules.KeyModule.class,
          CheckoutViewModel_HiltModules.KeyModule.class,
          ComplaintFileViewModel_HiltModules.KeyModule.class,
          ComplaintThreadViewModel_HiltModules.KeyModule.class,
          CustomerHomeViewModel_HiltModules.KeyModule.class,
          DepositsViewModel_HiltModules.KeyModule.class,
          DriverCodReconcileViewModel_HiltModules.KeyModule.class,
          DriverHomeViewModel_HiltModules.KeyModule.class,
          EventBookingViewModel_HiltModules.KeyModule.class,
          HiltWrapper_ActivityRetainedComponentManager_LifecycleModule.class,
          HiltWrapper_SavedStateHandleModule.class,
          ActivityCBuilderModule.class,
          ViewModelCBuilderModule.class,
          NotificationFeedViewModel_HiltModules.KeyModule.class,
          NotificationPrefsViewModel_HiltModules.KeyModule.class,
          OrderHistoryViewModel_HiltModules.KeyModule.class,
          OrderTrackingViewModel_HiltModules.KeyModule.class,
          ProfileViewModel_HiltModules.KeyModule.class,
          ReviewSubmitViewModel_HiltModules.KeyModule.class,
          SubscriptionCreateViewModel_HiltModules.KeyModule.class,
          SubscriptionDetailViewModel_HiltModules.KeyModule.class,
          SubscriptionListViewModel_HiltModules.KeyModule.class,
          VendorBankViewModel_HiltModules.KeyModule.class,
          VendorBusinessConfigViewModel_HiltModules.KeyModule.class,
          VendorCatalogViewModel_HiltModules.KeyModule.class,
          VendorComplianceViewModel_HiltModules.KeyModule.class,
          VendorDetailViewModel_HiltModules.KeyModule.class,
          VendorEarningsViewModel_HiltModules.KeyModule.class,
          VendorOnboardingViewModel_HiltModules.KeyModule.class,
          VendorOrderDetailViewModel_HiltModules.KeyModule.class,
          VendorSettingsViewModel_HiltModules.KeyModule.class,
          VendorSubscriptionsTodayViewModel_HiltModules.KeyModule.class,
          VendorTeamViewModel_HiltModules.KeyModule.class,
          VendorTodayViewModel_HiltModules.KeyModule.class,
          WalletViewModel_HiltModules.KeyModule.class
      }
  )
  @ActivityRetainedScoped
  public abstract static class ActivityRetainedC implements ActivityRetainedComponent,
      ActivityComponentManager.ActivityComponentBuilderEntryPoint,
      HiltWrapper_ActivityRetainedComponentManager_ActivityRetainedLifecycleEntryPoint,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends ActivityRetainedComponentBuilder {
    }
  }

  @Subcomponent(
      modules = {
          HiltWrapper_ActivityModule.class,
          HiltWrapper_DefaultViewModelFactories_ActivityModule.class,
          FragmentCBuilderModule.class,
          ViewCBuilderModule.class
      }
  )
  @ActivityScoped
  public abstract static class ActivityC implements MainActivity_GeneratedInjector,
      ActivityComponent,
      DefaultViewModelFactories.ActivityEntryPoint,
      HiltWrapper_HiltViewModelFactory_ActivityCreatorEntryPoint,
      FragmentComponentManager.FragmentComponentBuilderEntryPoint,
      ViewComponentManager.ViewComponentBuilderEntryPoint,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends ActivityComponentBuilder {
    }
  }

  @Subcomponent(
      modules = {
          AddressFormViewModel_HiltModules.BindsModule.class,
          AddressListViewModel_HiltModules.BindsModule.class,
          AuthViewModel_HiltModules.BindsModule.class,
          CartViewModel_HiltModules.BindsModule.class,
          CheckoutViewModel_HiltModules.BindsModule.class,
          ComplaintFileViewModel_HiltModules.BindsModule.class,
          ComplaintThreadViewModel_HiltModules.BindsModule.class,
          CustomerHomeViewModel_HiltModules.BindsModule.class,
          DepositsViewModel_HiltModules.BindsModule.class,
          DriverCodReconcileViewModel_HiltModules.BindsModule.class,
          DriverHomeViewModel_HiltModules.BindsModule.class,
          EventBookingViewModel_HiltModules.BindsModule.class,
          HiltWrapper_HiltViewModelFactory_ViewModelModule.class,
          NotificationFeedViewModel_HiltModules.BindsModule.class,
          NotificationPrefsViewModel_HiltModules.BindsModule.class,
          OrderHistoryViewModel_HiltModules.BindsModule.class,
          OrderTrackingViewModel_HiltModules.BindsModule.class,
          ProfileViewModel_HiltModules.BindsModule.class,
          ReviewSubmitViewModel_HiltModules.BindsModule.class,
          SubscriptionCreateViewModel_HiltModules.BindsModule.class,
          SubscriptionDetailViewModel_HiltModules.BindsModule.class,
          SubscriptionListViewModel_HiltModules.BindsModule.class,
          VendorBankViewModel_HiltModules.BindsModule.class,
          VendorBusinessConfigViewModel_HiltModules.BindsModule.class,
          VendorCatalogViewModel_HiltModules.BindsModule.class,
          VendorComplianceViewModel_HiltModules.BindsModule.class,
          VendorDetailViewModel_HiltModules.BindsModule.class,
          VendorEarningsViewModel_HiltModules.BindsModule.class,
          VendorOnboardingViewModel_HiltModules.BindsModule.class,
          VendorOrderDetailViewModel_HiltModules.BindsModule.class,
          VendorSettingsViewModel_HiltModules.BindsModule.class,
          VendorSubscriptionsTodayViewModel_HiltModules.BindsModule.class,
          VendorTeamViewModel_HiltModules.BindsModule.class,
          VendorTodayViewModel_HiltModules.BindsModule.class,
          WalletViewModel_HiltModules.BindsModule.class
      }
  )
  @ViewModelScoped
  public abstract static class ViewModelC implements ViewModelComponent,
      HiltViewModelFactory.ViewModelFactoriesEntryPoint,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends ViewModelComponentBuilder {
    }
  }

  @Subcomponent
  @ViewScoped
  public abstract static class ViewC implements ViewComponent,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends ViewComponentBuilder {
    }
  }

  @Subcomponent(
      modules = ViewWithFragmentCBuilderModule.class
  )
  @FragmentScoped
  public abstract static class FragmentC implements FragmentComponent,
      DefaultViewModelFactories.FragmentEntryPoint,
      ViewComponentManager.ViewWithFragmentComponentBuilderEntryPoint,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends FragmentComponentBuilder {
    }
  }

  @Subcomponent
  @ViewScoped
  public abstract static class ViewWithFragmentC implements ViewWithFragmentComponent,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends ViewWithFragmentComponentBuilder {
    }
  }
}
