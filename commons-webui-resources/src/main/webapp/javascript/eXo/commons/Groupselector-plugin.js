
(function($, undefined) {

  if (!window.location.origin) { // Some browsers (mainly IE) does not have this property, so we need to build it manually...
    window.location.origin = window.location.protocol + '//' + window.location.hostname + (window.location.port ? (':' + window.location.port) : '');
  }

  if(!Array.prototype.remove) {
    Array.prototype.remove = function() {
      var items = arguments, L = items.length, what, index;
      while (L && this.length) {
        what = items[--L];
        while ((index = $.inArray(what, this)) !== -1) {
          this.splice(index, 1);
        }
      }
      return this;
    };
  }
  if(!Array.prototype.contains) {
    Array.prototype.contains = function() {
      var item = arguments;
      return (item && $.inArray(item, this) !== -1) ? true : false;
    };
  }
  var KEY = {
    BACKSPACE : 8,
    TAB : 9,
    RETURN : 13,
    ESC : 27,
    LEFT : 37,
    UP : 38,
    RIGHT : 39,
    DOWN : 40,
    DELETE : 46,
    MENTION : 64,
    COMMA : 188,
    SPACE : 32,
    HOME : 36,
    END : 35
  }; // Keys "enum"
  var defaultSettings = {
    showAvatars : true,
    firstShowAll : false,
    selectFirst : true,
    idActions : "",
    actionLink : null,
    i18n : {
      inLabel : "in",
      anyLabel : "Any",
      userLabel : "Users",
      groupLabel : "Groups",
      noMatchLabel : "No match"
    },
    memberships : {},
    classes : {
      autoCompleteItemActive : "active"
    },
    callBackValue : null,
    validateValue : function(val) {
      return true;
    },
    messages : {
      helpSearch: 'Type to start searching for users.',
      searching: 'Searching for ',
      foundNoMatch : 'Found no matching users for '
    },
    templates : {
      wrapper : ('<div class="exo-group-selector"></div>'),
      autocompleteList : ('<ul class="autocomplete-menu dropdown-menu"><li></li></ul>'),
      autocompleteListItem : ('<li class="data" data-id="$id" data-type="$type" data-display="$display"><a href="javascript:void(0)">$avatar$display</a></li>'),
      //
      itemUser : ('<span class="item uiMention user"><span>$value</span><i class="uiIconClose uiIconLightGray"></i></span>'),
      itemGroup : ('<span class="item dropdown membership"><span class="uiMention"><span>$membership</span>' + 
                   '<i data-toggle="dropdown" class="uiIconMiniArrowDown uiIconLightGray dropdown-toggle"></i></span> $sv ' +
                   '<span class="uiMention"><span class="group">$group</span><i class="uiIconClose uiIconLightGray"></i></span>'),
      memberShipList : ('<ul class="dropdown-menu"><li></li></ul>'),
      memberShipListItem : ('<li class="item" data-id="$id"><a href="javascript:void(0);">$label</a></li>')
    }
  };
  
  var groupSelector = function(settings) {
    settings = $.extend(true, {}, defaultSettings, settings);
    settings.templates.itemGroup = settings.templates.itemGroup.replace('$sv', settings.i18n.inLabel);
    if(settings.showAvatars == false) {
      settings.templates.autocompleteListItem = settings.templates.autocompleteListItem.replace('$avatar', '');
    }
    //
    var dataStorage = [];
    var userList = {};window.userList = userList;
    var groupList = {};window.groupList = groupList;
    //
    var jtarget, jinput, jwrapper, jwInput, ulWrapper, ulComplete, beforeInput='';
    
    function setupInput() {
      jinput.on('keydown', onInputKeyDown);
      jinput.on('keypress', onInputKeyPress);
      jinput.on('keyup', onInputKeyUp);
      jinput.on('focus', onInputFocus);
      jinput.on('paste', onInputPaste);
      jinput.on('blur', onInputBlur);
    }
    
    function addStorage(val) {
      if(!dataStorage.contains(val)) {
        dataStorage.push(val);
        jtarget.val(getValue());
      }
    }
    function removeStorage(val) {
      dataStorage.remove(val);
      jtarget.val(getValue());
    }
    function clearStorage(val) {
      dataStorage = [];
      jtarget.val("");
    }

    function getValue() {
      return dataStorage.join();
    }
    function setValue(objects) {
      //membership ==>  { 'any:/platform/user' : {type:'Any', group: 'Platform user'} }
      //user ==> { userId : 'display name' }
      $.each(objects, function(key){
        addStorage(key);
        if(key.indexOf('/') >= 0) {
          groupList[key] = objects[key];
        } else {
          userList[key] = objects[key];
        }
      });
      //
      buildValues();
    }
    
    function resetItems() {
      jwrapper.find('.user').remove();
      jwrapper.find('.membership').remove();
    }
    
    function buildValues() {
      //
      resetItems();
      //
      var template = settings.templates.itemUser;
      $.each(userList, function(key){
        //
        buildItemUserList(key);
      });
      //
      $.each(groupList, function(key) {
        //
        buildItemGroupList(key);
      });
      
      jwrapper.find('i.uiIconClose').on('click', function(evt) {
        removeItem($(this).parents('.user:first'));
        removeItem($(this).parents('.membership:first'));
      });
    }
    
    function removeItem(item) {
      if(item.length > 0) {
        var id = item.data('id');
        //
        removeStorage(id);
        //
        delete userList[id];
        delete groupList[id];
        //
        item.remove();
      }
    }
    
    function addItem(item) {
      if(item && $.isPlainObject(item)) {
        
      }
    }
    
    function onClickDropDownMembership(evt) {
      var jitem = $(this).parents('.membership');
      var lis = jitem.find('ul.dropdown-menu').find('li');
      if(lis.length == 1) {
        lis = jitem.find('ul.dropdown-menu').html(ulWrapper.html()).find('li');
        lis.on('click', function(e) {
          $(this).parents('.uiMention:first').find('span:first').html($(this).text());
          var parent = $(this).parents('.membership:first');//
          var oldid = parent.data('id');
          parent.data('id', $(this).data('id') + oldid.substring(oldid.indexOf(':') + 1));
          //
          var gLabel = groupList[oldid].group;
          delete groupList[oldid];
          groupList[parent.data('id')] = {"type" : $(this).html(), "group" : gLabel};
          //
          removeStorage(oldid);
          addStorage(parent.data('id'));
          //
          itemWidth(parent, gLabel);
        })
      }
    }
    
    function buildItemGroupList(key) {
      var item = groupList[key];
      var jitem = $(settings.templates.itemGroup.replace('$membership', item.type).replace('$group', item.group)).data('id', key);
      //
      $(settings.templates.memberShipList).insertAfter(jitem.find('i.uiIconMiniArrowDown:first'));
      //
      jitem.insertBefore(jwInput);
      //
      itemWidth(jitem, item.group);
      //
      jitem.find('i.uiIconMiniArrowDown:first').on('click', onClickDropDownMembership);
    }
    
    function itemWidth(jitem, text) {
      jitem.addClass('before-membership');
      var mW = jwrapper.width();
      var fW = jitem.width();
      var group = jitem.find('.group:first').text(text);
      var i = text.length - 3;
      while(fW > mW) {
        group.html(text.substring(0, i) + '...');
        fW = jitem.width();
        --i;
      }
      jitem.removeClass('before-membership');
    }
    
    function buildItemUserList(key) {
      var jitem = $(settings.templates.itemUser.replace('$value', userList[key])).data('id', key);
      jitem.insertBefore(jwInput);
    }
    ///////////////////////// Auto complete \\\\\\\\\\\\\\\\\\\\\\\\\\\\
    function onInputFocus(e) {
      console.log('onInputFocus');
    }
    function onInputKeyDown(e) {
      var keyCode = (e.which || e.keyCode);
      if(keyCode == KEY.RETURN && validateValue() == false) {
        e.stopPropagation();
        return;
      }
      //console.log('onInputKeyDown ' + keyCode);
      beforeInput = jinput.val();
    }
    function onInputKeyPress(e) {
      //console.log('onInputKeyPress');
    }
    function onInputKeyUp(e) {
      var keyCode = (e.which || e.keyCode);
      var active = ulComplete.find('li.active:first');
      //RETURN
      if(keyCode == KEY.RETURN) {
        e.stopPropagation();
        //active item
        if(active.length > 0) {
          applyCompleteItem(active);
        }
        
        if(validateValue() == false) {
          return;
        }
      }
      //BACKSPACE
      if(keyCode == KEY.BACKSPACE) {
        e.stopPropagation();
        //remove selected mention
        var item = jwrapper.find('span.selected');
        if(item.length > 0) {
          item.find('.uiIconClose').trigger('click');
          return;
        } else {
          // remove last mention
          if(beforeInput == '') {
            var current = jwInput.prev();
            if(current.hasClass('item')) {
              current.find('.uiIconClose').trigger('click');
            }
            return;
          }
        }
      }
      //UP DOWN
      if(active.length > 0 && (keyCode == KEY.UP || keyCode == KEY.DOWN) && ulComplete.find('li.data').length > 1) {
        var nextSelect = active;
        var activeId = active.data('id');
        if(keyCode == KEY.UP) {
          if(ulComplete.find('li.data:first').data('id') === activeId) {
            nextSelect = ulComplete.find('li.data:last');
          } else {
            var all = ulComplete.find('li.data');
            $.each(all, function(it) {
              if($(this).data('id') === activeId) {
                nextSelect = $(all[it-1]);
              }
            });
          }
        } else if(keyCode == KEY.DOWN) {
          if(ulComplete.find('li.data:last').data('id') === activeId) {
            nextSelect = ulComplete.find('li.data:first');
          } else {
            var all = ulComplete.find('li.data');
            $.each(all, function(it) {
              if($(this).data('id') === activeId) {
                nextSelect = $(all[it+1]);
              }
            });
          }
        }
        if(active[0] !== nextSelect[0]) {
          active.removeClass('active');
          nextSelect.addClass('active');
        }
        //
        return;
      }
      //LEFT RIGHT item
      if(beforeInput == '' && jwrapper.find('span.item').length > 0 && (keyCode == KEY.LEFT || keyCode == KEY.RIGHT)) {
        var item = jwrapper.find('span.selected');
        if(keyCode == KEY.LEFT) {
          if(item.length == 0) {
            jwrapper.find('span.item:last').addClass('selected');
          } else {
            if(jwrapper.find('span.item:first')[0] != item[0]) {
              item.removeClass('selected').prev().addClass('selected');
            }
          }
        } else {
          if(item.length > 0) {
            item.removeClass('selected');
            if(jwrapper.find('span.item:last')[0] != item[0]) {
              item.next().addClass('selected');
            }
          }
        }
        //
        return;
      }
      // DELETE
      if((keyCode == KEY.DELETE) && jwrapper.find('span.selected').length > 0) {
        jwrapper.find('span.selected').find('.uiIconClose').trigger('click');
        //
        return;
      }
      // ESC
      if((keyCode == KEY.ESC) && jwrapper.find('span.selected').length > 0) {
        jwrapper.find('span.selected').removeClass('selected');
        //
        return;
      }
      //
      jwrapper.find('span.selected').removeClass('selected');
      var input = jinput.val();
      //
      search(input);
      
    }

    function onInputBlur(e) {
      console.log('onInputBlur');
    }
    function onInputPaste(e) {
      console.log('onInputPaste');
    }
    
    function populateDropdown(query, datas) {
      //<li class="data" data-id="$id" data-type="$type" data-display="$display">$display</li>
      //datas = [{'demo' : {name : 'Demo gtn', avatar: 'avatar url'} }, {'/platform/user':  {name : 'Platform user', avatar: 'avatar url'}} ]
      var groupMenus = {};
      var userMenus = {};
      var dataInfo = {};
      //
      clearAutocompleteMenu();
      if(query === '') {
        hideAutocompleteMenu();
        return false;
      }
      var isArray = $.isArray(datas);
      if(isArray) {
        $.each(datas, function(index){
          dataInfo = $.extend(true, {}, dataInfo, this);
        });
      }
      //
      $.each(dataInfo, function(key){
        if(key.indexOf('/') >= 0) {
          groupMenus[key] = datas[key];
        } else {
          userMenus[key] = datas[key];
        }
      });
      
      //
      autocompleteItems(userMenus, 'user', settings.i18n.userLabel);
      //
      autocompleteItems(groupMenus, 'group', settings.i18n.groupLabel);
      // apply behavior menu
      ulComplete.find('li.data:first').addClass('active');
      
      //
      return true;
    }
    
    function autocompleteItems(datas, type, label) {
      //
      ulComplete.append($('<li class="groupLabel">' + label + '</li>'));
      if($.isEmptyObject(datas)) {
        ulComplete.append($('<li class="noMatch">' + settings.i18n.noMatchLabel + '</li>'));
      } else {
        $.each(datas, function(key){
          var info = datas[key];
          var li = settings.templates.autocompleteListItem.replace('$id', key).replace(/\$display/g, info.name);
          if (settings.showAvatars && info.name) {
            if (info.avatar) {
              li = li.replace('$avatar','<i class="avatarSmall"><img src="' + info.name + '"/></i> ');
            } else {
              li = li.replace('$avatar','<i class="avatarSmall"><img src="/social-resources/skin/images/ShareImages/UserAvtDefault.png"/></i> ');
            }
          }
          li = li.replace()
          li = $(li.replace('$type', type ));
          li.on('mousedown', function(evt) {
            applyCompleteItem($(this));
          }).on('mouseover', function(evt) {
            var active = ulComplete.find('li.active:first');
            if(this !== active[0]) {
              active.removeClass('active');
              $(this).addClass('active');
            }
          });
          //
          ulComplete.append(li);
        });
      }
    }
    
    function applyCompleteItem(item) {
      var type = item.data('type');
      var object = {};
      if(type === 'user') {
        object[item.data('id')] = item.data('display');
      } else {
        //
        object['any:' + item.data('id')] = {type: settings.i18n.anyLabel, group: item.data('display')};
      }
      //
      setValue(object);
      //
      hideAutocompleteMenu();
      clearAutocompleteMenu();
      //
      jinput.val('');
      //
      var T = setTimeout(function() {
        jwrapper.trigger('click');
        clearTimeout(T);
      }, 100);
    }
    
    function search(query) {
      //before
      //populateDropdown('', {});
      //searching
      /*
      settings.onDataRequest.call(this, function(responseData) {
        if(populateDropdown(query, responseData)) {
          showAutocompleteMenu();
        }
      });
      */
      if(settings.url && settings.url.length > 0) {
        var url = settings.url;
        if(url.indexOf(window.location.origin) < 0) {
          url = window.location.origin + ((url.indexOf('/') !== 0) ? '/' + url : url);
        }
        jQuery.getJSON(settings.url + query, function(response) {
          if(populateDropdown(query, response)) {
            showAutocompleteMenu();
          }
        });
      }
    }
    
    function showAutocompleteMenu() {
      jwrapper.addClass('open');
    }

    function hideAutocompleteMenu() {
      jwrapper.removeClass('open');
    }

    function clearAutocompleteMenu() {
      ulComplete.html('');
    }
    
    function validateValue() {
      var input = jinput.val();
      if(settings.validateValue && settings.validateValue(input) == false) {
        return false;
      }
      return true;
    }
    
    // Public methods
    return {
      init : function(wrapper) {
        jwrapper = $(wrapper);
        jinput = jwrapper.find('input.target-input:first');
        jwInput = jwrapper.find('.w-input');
        jtarget = jwrapper.find('input#' + jwrapper.attr('id').replace('wrapper-', '') + ':first');
        jwrapper.on('click', function() {
          jwInput.hide();
          jinput.show().trigger('focus');
          jinput[0].focus();
          jwrapper.addClass('uneditable-input-focus');
          if(ulComplete.find('li').length > 1) {
            showAutocompleteMenu();
          }
        }).focusout(function() {
          if(jinput.val().trim().length == 0) {
            jinput.hide();
            jwInput.show();
          }
          jwrapper.removeClass('uneditable-input-focus');
          hideAutocompleteMenu();
        });
        // build list
        ulWrapper = $('<ul class="ul-wrapper" style="display:none"></ul>');
        $.each(settings.memberships, function(key) {
          ulWrapper.append(
              $(settings.templates.memberShipListItem.replace('$id', key).replace('$label', settings.memberships[key]))
          );
        });
        // add autocomplete
        ulComplete = $(settings.templates.autocompleteList).appendTo(jwrapper);
        //
        setupInput();
        // action mention
        if (settings.idActions && settings.idActions.length > 0) {
          var action = null;
          if(typeof settings.idActions === 'string') {
            action = $('#'+settings.idActions);
          } else if(typeof settings.idActions === 'object') {
            action = $(settings.idActions);
          }
          if(action && action.length > 0) {
            action.on('click', function(e) {
              e.stopPropagation();
              //
            });
          }
        }
      },
      getVal : function(method) {
        if (method && $.isFunction(method)) {
          var value = getValue();
          method.call(this, value);
        }
        if (settings.callBackValue && $.isFunction(settings.callBackValue)) {
          var value = getValue();
          settings.callBackValue.call(this, value);
        }
      },
      setVal : function(method) {
        if (method && $.isPlainObject(method)) {
          setValue(method);
        }
      },
      reset : function() {
        clearStorage();
        userList = {};
        groupList = {};
        resetItems();
      },
      showButton : function() {
        var action = $('#' + settings.idAction);
        if (action.length > 0 && action.attr('disabled') === 'disabled') {
          action.removeAttr('disabled');
        }
      }
    };
  };

  $.fn.groupSelector = function(method, settings) {

    var outerArguments = arguments;
    if (typeof method === 'object' || !method) {
      settings = method;
    }
    return this.each(function() {
      var instance = $.data(this, 'groupSelector') || $.data(this, 'groupSelector', new groupSelector(settings));
      if ($.isFunction(instance[method])) {
        return instance[method].apply(this, Array.prototype.slice.call(outerArguments, 1));
      } else if (typeof method === 'object' || !method) {
        return instance.init.call(this, this);
      } else {
        $.error('Method ' + method + ' does not exist');
      }
    });
  };
})(jQuery);